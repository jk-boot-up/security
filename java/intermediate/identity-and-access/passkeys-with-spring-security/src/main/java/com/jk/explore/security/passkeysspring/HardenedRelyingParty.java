package com.jk.explore.security.passkeysspring;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialRequestOptionsRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

/**
 * What Spring Security leaves to the bank. It checks the signature, the challenge, the origin and the relying party id, and keeps the passkeys, but it does not insist that the
 * user was verified, and it does not compare the signature counter. This wrapper adds both: it refuses a sign-in without user verification, and it suspends a passkey whose counter
 * does not go up, after the signature has proved that whoever signed holds the key. A counter of 0 in the stored record and in the answer means that the passkey does not count
 * (a synced passkey), and is let through.
 */
final class HardenedRelyingParty implements WebAuthnRelyingPartyOperations {

    private static final int FLAGS = 32;
    private static final int COUNTER = 33;
    private static final int USER_VERIFIED = 0x04;

    private final WebAuthnRelyingPartyOperations delegate;
    private final UserCredentialRepository credentials;
    private final Set<Bytes> suspended = ConcurrentHashMap.newKeySet();
    private final java.util.List<String> refusals = new java.util.concurrent.CopyOnWriteArrayList<>();

    HardenedRelyingParty(WebAuthnRelyingPartyOperations delegate, UserCredentialRepository credentials) {
        this.delegate = delegate;
        this.credentials = credentials;
    }

    @Override
    public PublicKeyCredentialCreationOptions createPublicKeyCredentialCreationOptions(PublicKeyCredentialCreationOptionsRequest request) {
        return delegate.createPublicKeyCredentialCreationOptions(request);
    }

    @Override
    public CredentialRecord registerCredential(RelyingPartyRegistrationRequest request) {
        return delegate.registerCredential(request);
    }

    @Override
    public PublicKeyCredentialRequestOptions createCredentialRequestOptions(PublicKeyCredentialRequestOptionsRequest request) {
        return delegate.createCredentialRequestOptions(request);
    }

    @Override
    public PublicKeyCredentialUserEntity authenticate(RelyingPartyAuthenticationRequest request) {
        Bytes id = request.getPublicKey().getRawId();
        byte[] authenticatorData = request.getPublicKey().getResponse().getAuthenticatorData().getBytes();
        if (suspended.contains(id)) {
            throw refuse("passkey suspended after a counter went backwards");
        }
        if ((authenticatorData[FLAGS] & USER_VERIFIED) == 0) {
            throw refuse("user verification required");
        }
        CredentialRecord before = credentials.findByCredentialId(id);
        long storedCounter = before == null ? 0 : before.getSignatureCount();
        PublicKeyCredentialUserEntity user = delegate.authenticate(request);   // the signature, the challenge, the origin and the relying party id
        long presented = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(authenticatorData, COUNTER, 4).getInt());
        if ((storedCounter > 0 || presented > 0) && presented <= storedCounter) {
            suspended.add(id);
            throw refuse("counter " + presented + " is not above " + storedCounter + ": possible cloned authenticator");
        }
        return user;
    }

    private BadCredentialsException refuse(String reason) {
        refusals.add(reason);
        return new BadCredentialsException(reason);
    }

    /** Why this wrapper refused sign-ins, most recent last. The customer is only ever told that sign-in failed; the log keeps the reason. */
    java.util.List<String> refusals() {
        return java.util.List.copyOf(refusals);
    }

    boolean isSuspended(Bytes id) {
        return suspended.contains(id);
    }
}
