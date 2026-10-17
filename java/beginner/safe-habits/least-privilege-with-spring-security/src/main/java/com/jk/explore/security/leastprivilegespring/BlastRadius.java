package com.jk.explore.security.leastprivilegespring;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

/** What a stolen credential can do: ask Spring Security's own authorization manager about every action in the store. */
final class BlastRadius {

    private BlastRadius() {
    }

    static Authentication token(Catalogue.Account account) {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(
                java.util.stream.Stream.concat(java.util.stream.Stream.of("ROLE_" + account.role()), account.permissions().stream())
                        .toArray(String[]::new));
        return UsernamePasswordAuthenticationToken.authenticated(account.name(), null, authorities);
    }

    static int reach(Catalogue.Account account) {
        Authentication token = token(account);
        int count = 0;
        for (String permission : Catalogue.PERMISSIONS) {
            if (AuthorityAuthorizationManager.hasAuthority(permission).authorize(() -> token, null).isGranted()) {
                count++;
            }
        }
        return count;
    }
}
