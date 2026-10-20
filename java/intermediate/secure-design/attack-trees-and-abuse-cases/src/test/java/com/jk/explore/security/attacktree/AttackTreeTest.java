package com.jk.explore.security.attacktree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AttackTreeTest {

    private static Node.Leaf l(String id, double cost, double p) {
        return Node.leaf(id, id, cost, p, "");
    }

    @Test
    void costsAddOnAndAndTheCheapestWinsOnOr() {
        Node t = Node.or("goal", Node.and("both", l("a", 100, 1), l("b", 50, 1)), l("c", 200, 1));
        Analysis a = Analysis.of(t);
        assertEquals(150, a.minCost());
        assertEquals(List.of("a", "b"), a.cheapestPath());
    }

    @Test
    void probabilitiesMultiplyOnAndAndCombineOnOr() {
        assertEquals(0.25, Analysis.of(Node.and("g", l("a", 1, 0.5), l("b", 1, 0.5))).probability(), 1e-9);
        assertEquals(0.75, Analysis.of(Node.or("g", l("a", 1, 0.5), l("b", 1, 0.5))).probability(), 1e-9);
    }

    @Test
    void aStepThatCannotWorkBlocksItsBranchButNotTheOthers() {
        Node t = Node.or("g", Node.and("blocked", l("a", 1, 0), l("b", 1, 1)), l("c", 500, 0.1));
        Analysis a = Analysis.of(t);
        assertEquals(500, a.minCost());
        assertEquals(List.of("c"), a.cheapestPath());
        assertFalse(Analysis.of(Node.and("g", l("a", 1, 0), l("b", 1, 1))).possible());
        assertFalse(Analysis.of(l("x", 1, 0)).possible());
    }

    @Test
    void theBaselineTreeIsCheapToAttack() {
        Analysis a = Analysis.of(BankTree.baseline());
        assertEquals(50, a.minCost());
        assertEquals(List.of("B1"), a.cheapestPath());
        assertEquals(0.97, a.probability(), 0.005);
    }

    @Test
    void eachBranchHasItsOwnPrice() {
        List<Node> branches = ((Node.Gate) BankTree.baseline()).children();
        assertEquals(100, Analysis.of(branches.get(0)).minCost());
        assertEquals(50, Analysis.of(branches.get(1)).minCost());
        assertEquals(20_000, Analysis.of(branches.get(2)).minCost());
        assertEquals(6_000, Analysis.of(branches.get(3)).minCost());
    }

    @Test
    void aDefenceChangesOnlyTheLeavesItNames() {
        Defence own = Defence.all().stream().filter(d -> d.id().equals("OWN")).findFirst().orElseThrow();
        Analysis after = Analysis.of(own.applyTo(BankTree.baseline()));
        assertEquals(100, after.minCost());
        assertTrue(after.probability() < Analysis.of(BankTree.baseline()).probability());
        Defence csp = Defence.all().stream().filter(d -> d.id().equals("CSP")).findFirst().orElseThrow();
        Node.Leaf a4a = (Node.Leaf) find(csp.applyTo(BankTree.baseline()), "A4a");
        assertEquals(6_000, a4a.cost());
        assertEquals(0.02, a4a.probability(), 1e-9);
        assertEquals(0.5, ((Node.Leaf) find(csp.applyTo(BankTree.baseline()), "A4b")).probability());
    }

    private static Node find(Node n, String id) {
        if (n instanceof Node.Leaf leaf) {
            return leaf.id().equals(id) ? leaf : null;
        }
        for (Node c : ((Node.Gate) n).children()) {
            Node f = find(c, id);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    @Test
    void oneDefenceAloneNeverFixesTheTree() {
        for (Planner.Step s : Planner.rank(BankTree.baseline(), Defence.all())) {
            assertTrue(s.after().minCost() <= 100, s.defence().id());
        }
        assertEquals("OWN", Planner.rank(BankTree.baseline(), Defence.all()).get(0).defence().id());
    }

    @Test
    void thePlanRaisesTheCheapestPathStepByStepToTheTarget() {
        List<Planner.Step> plan = Planner.plan(BankTree.baseline(), Defence.all(), 5_000);
        assertEquals(List.of("OWN", "CSRF", "PASSKEY", "CSP"), plan.stream().map(s -> s.defence().id()).toList());
        assertEquals(List.of(100.0, 100.0, 2_000.0, 6_000.0), plan.stream().map(s -> s.after().minCost()).toList());
        assertEquals(55_000, plan.get(plan.size() - 1).totalSpent());
        double previous = 1.0;
        for (Planner.Step s : plan) {
            assertTrue(s.after().probability() < previous);
            previous = s.after().probability();
        }
    }

    @Test
    void theAbuseCasesFailOnTheWeakBankAndPassWithEveryDefence() {
        BankConfig all = BankConfig.weak();
        for (Defence d : Defence.all()) {
            all = all.with(d.configChange());
        }
        for (AbuseCase c : AbuseCase.all()) {
            assertFalse(c.passes(BankConfig.weak()), c.id());
            assertTrue(c.passes(all), c.id());
        }
        assertEquals(8, AbuseCase.all().size());
    }

    @Test
    void eachDefenceFixesTheAbuseCasesOfTheLeavesItRemoves() {
        for (Defence d : Defence.all()) {
            BankConfig only = BankConfig.weak().with(d.configChange());
            String fixed = AbuseCase.all().stream().filter(c -> c.passes(only)).map(AbuseCase::id).collect(Collectors.joining(","));
            assertFalse(fixed.isEmpty(), d.id() + " fixes at least one abuse case");
        }
        assertTrue(AbuseCase.all().get(4).passes(BankConfig.weak().with("ownership")));
        assertFalse(AbuseCase.all().get(0).passes(BankConfig.weak().with("otp")), "a phished one-time code still works");
        assertTrue(AbuseCase.all().get(0).passes(BankConfig.weak().with("passkey")));
    }

    @Test
    void everyAbuseCaseIsLinkedToALeafAndTheTreeHasOneWithoutACase() {
        assertEquals(14, AttackTreeDemo.countLeaves(BankTree.baseline()));
        assertEquals(13, AttackTreeDemo.countCovered(BankTree.baseline()));
        List<String> ids = AbuseCase.all().stream().map(AbuseCase::id).toList();
        assertTrue(ids.containsAll(List.of("AC1", "AC2", "AC3", "AC4", "AC5", "AC6", "AC7", "AC8")));
    }

    @Test
    void theFullDefenceSetLeavesOnlyExpensiveAttacks() {
        Node t = BankTree.baseline();
        for (Defence d : Defence.all()) {
            t = d.applyTo(t);
        }
        Analysis a = Analysis.of(t);
        assertTrue(a.possible());
        assertEquals(6_000, a.minCost());
        assertTrue(a.probability() < 0.2);
    }
}
