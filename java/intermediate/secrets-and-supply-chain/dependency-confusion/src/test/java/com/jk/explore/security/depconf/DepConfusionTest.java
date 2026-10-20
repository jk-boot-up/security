package com.jk.explore.security.depconf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DepConfusionTest {

    private static final Map<String, VersionRange> DEPS = DepConfusionDemo.dependencies();

    private static Repository squattedPublic() {
        return DepConfusionDemo.publicRepo()
                .publish(Artifact.of("shop-pricing", "99.0.0", "x").withInstallScript(DepConfusionDemo.ATTACK))
                .publish(Artifact.of("shop-newlib", "1.0.0", "x").withInstallScript(DepConfusionDemo.ATTACK));
    }

    @Test
    void versionsCompareNumberByNumber() {
        assertTrue(Version.parse("1.10.0").compareTo(Version.parse("1.9.0")) > 0);
        assertTrue(Version.parse("2.0.0").compareTo(Version.parse("1.99.99")) > 0);
        assertEquals("1.4.2", Version.parse("1.4.2").toString());
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1.4"));
    }

    @Test
    void rangesIncludeTheLowerBoundAndExcludeTheUpperBound() {
        VersionRange r = VersionRange.major(1);
        assertTrue(r.contains(Version.parse("1.0.0")));
        assertTrue(r.contains(Version.parse("1.99.0")));
        assertFalse(r.contains(Version.parse("2.0.0")));
        assertTrue(VersionRange.exactly("3.1.0").contains(Version.parse("3.1.0")));
        assertFalse(VersionRange.exactly("3.1.0").contains(Version.parse("3.1.1")));
        assertTrue(VersionRange.atLeast(1).contains(Version.parse("99.0.0")));
        assertEquals("[1.0.0, 2.0.0)", VersionRange.major(1).toString());
        assertEquals(">= 1.0.0", VersionRange.atLeast(1).toString());
    }

    @Test
    void theDigestCoversNameVersionContentAndScript() {
        Artifact a = Artifact.of("x", "1.0.0", "c");
        assertEquals(64, a.digest().length());
        assertNotEquals(a.digest(), Artifact.of("x", "1.0.0", "d").digest());
        assertNotEquals(a.digest(), Artifact.of("y", "1.0.0", "c").digest());
        assertNotEquals(a.digest(), Artifact.of("x", "1.0.1", "c").digest());
        assertNotEquals(a.digest(), a.withInstallScript("echo").digest());
    }

    @Test
    void highestVersionWinsLetsTheAttackerIn() {
        Build.Report r = new Build(new HighestVersionResolver(), List.of(DepConfusionDemo.privateRepo(), squattedPublic())).run(DEPS);
        assertEquals(1, r.scriptsRun().size());
        assertTrue(r.installed().stream().anyMatch(x -> x.artifact().name().equals("shop-pricing") && x.source().isPublic()));
    }

    @Test
    void repositoryOrderProtectsExistingNamesButNotMissingOnesOrExactVersionCopies() {
        Repository open = squattedPublic();
        Repository internal = DepConfusionDemo.privateRepo();
        assertTrue(new Build(new FirstRepositoryResolver(), List.of(internal, open)).run(DEPS).scriptsRun().isEmpty());
        Map<String, VersionRange> withNew = new java.util.LinkedHashMap<>(DEPS);
        withNew.put("shop-newlib", VersionRange.any());
        assertEquals(1, new Build(new FirstRepositoryResolver(), List.of(internal, open)).run(withNew).scriptsRun().size());
        assertEquals(1, new Build(new FirstRepositoryResolver(), List.of(open, internal)).run(DEPS).scriptsRun().size());
    }

    @Test
    void publicFirstIsFooledByAnExactVersionCopy() {
        Repository open = DepConfusionDemo.publicRepo().publish(Artifact.of("shop-audit", "3.1.0", "copy").withInstallScript(DepConfusionDemo.ATTACK));
        Build.Report r = new Build(new FirstRepositoryResolver(), List.of(open, DepConfusionDemo.privateRepo())).run(DEPS);
        assertTrue(r.installed().stream().anyMatch(x -> x.artifact().name().equals("shop-audit") && x.source().isPublic()));
    }

    @Test
    void contentFilteringKeepsInternalNamesInternalAndFailsClosed() {
        Resolver f = new ContentFilteringResolver(n -> n.startsWith("shop-"));
        List<Repository> repos = List.of(squattedPublic(), DepConfusionDemo.privateRepo());
        Build.Report r = new Build(f, repos).run(DEPS);
        assertTrue(r.scriptsRun().isEmpty());
        assertTrue(r.errors().isEmpty());
        assertEquals(3, r.installed().size());
        Map<String, VersionRange> withNew = new java.util.LinkedHashMap<>(DEPS);
        withNew.put("shop-newlib", VersionRange.any());
        Build.Report r2 = new Build(f, repos).run(withNew);
        assertTrue(r2.scriptsRun().isEmpty());
        assertEquals(1, r2.errors().size());
        assertTrue(r2.errors().get(0).startsWith("shop-newlib is an internal name"));
    }

    @Test
    void publicNamesAreNeverLookedUpInThePrivateRepository() {
        Resolver f = new ContentFilteringResolver(n -> n.startsWith("shop-"));
        Repository evilPrivate = new Repository("company-private", false).publish(Artifact.of("json-lib", "2.0.1", "x"));
        assertThrows(ResolutionException.class, () -> f.resolve("json-lib", VersionRange.major(2), List.of(evilPrivate)));
    }

    @Test
    void theLockfileCatchesTheWrongSourceOrChangedBytesAndUnknownNames() {
        Lockfile lock = new Lockfile();
        new Build(new HighestVersionResolver(), List.of(DepConfusionDemo.privateRepo(), DepConfusionDemo.publicRepo())).run(DEPS).installed().forEach(lock::record);
        assertEquals(3, lock.size());
        Resolver v = new VerifyingResolver(new HighestVersionResolver(), lock);
        assertEquals(1, new Build(v, List.of(DepConfusionDemo.privateRepo(), squattedPublic())).run(DEPS).errors().size());
        Repository changed = new Repository("company-private", false).publish(Artifact.of("shop-pricing", "1.4.2", "changed")).publish(Artifact.of("shop-audit", "3.1.0", "internal audit client"));
        Build.Report r = new Build(new VerifyingResolver(new ContentFilteringResolver(n -> n.startsWith("shop-")), lock), List.of(changed, DepConfusionDemo.publicRepo())).run(DEPS);
        assertEquals(1, r.errors().size());
        assertTrue(r.errors().get(0).contains("does not match the lockfile"));
        assertThrows(ResolutionException.class, () -> v.resolve("surprise", VersionRange.any(), List.of(DepConfusionDemo.publicRepo())));
    }

    @Test
    void nameWatchListsInternalNamesThatThePublicRegistryHas() {
        assertEquals(List.of("shop-newlib", "shop-pricing"), NameWatch.shadowedNames(DepConfusionDemo.INTERNAL, squattedPublic()));
        assertEquals(List.of(), NameWatch.shadowedNames(DepConfusionDemo.INTERNAL, DepConfusionDemo.publicRepo()));
    }

    @Test
    void aResolverThatFindsNothingSaysSo() {
        assertThrows(ResolutionException.class, () -> new HighestVersionResolver().resolve("nothing", VersionRange.any(), Set.<Repository>of().stream().toList()));
        assertThrows(ResolutionException.class, () -> new FirstRepositoryResolver().resolve("nothing", VersionRange.any(), List.of(DepConfusionDemo.publicRepo())));
    }
}
