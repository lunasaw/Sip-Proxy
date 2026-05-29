/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *
 * .
 *
 */
package io.github.lunasaw.gbproxy.server.transmit.request.register;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Random;

import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.message.Request;

import io.github.lunasaw.sip.common.utils.SipDigestUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements the HTTP digest authentication method server side functionality.
 *
 * <p>v1.7.x 起：摘要算法按 {@link AuthorizationHeader#getAlgorithm()} 选择，支持
 * MD5（RFC 3261 默认）与 SM3（GBT-28181-2022 §8.3 推荐）。算法名缺失时回落到
 * {@link #DEFAULT_ALGORITHM}。
 *
 * @author M. Ranganathan
 * @author Marc Bednarek
 */

@Slf4j
public class DigestServerAuthenticationHelper {

    public static final MessageDigest messageDigest;
    public static final String DEFAULT_ALGORITHM = "MD5";
    public static final String DEFAULT_SCHEME = "Digest";

    /**
     * Default constructor.
     *
     * @throws NoSuchAlgorithmException
     */
    static {
        try {
            messageDigest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate the challenge string.
     *
     * @return a generated nonce.
     */
    public static String generateNonce() {
        long time = Instant.now().toEpochMilli();
        Random rand = new Random();
        long pad = rand.nextLong();
        String nonceString = Long.valueOf(time).toString()
                + Long.valueOf(pad).toString();
        return SipDigestUtils.digestHex(DEFAULT_ALGORITHM, nonceString);
    }


    /**
     * Authenticate the inbound request.
     *
     * @param request        - the request to authenticate.
     * @param hashedPassword -- the {algorithm}(username:realm:plaintext password) hashed string.
     * @return true if authentication succeded and false otherwise.
     */
    public static boolean doAuthenticateHashedPassword(Request request, String hashedPassword) {
        AuthorizationHeader authHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (authHeader == null) {
            return false;
        }
        String realm = authHeader.getRealm();
        String username = authHeader.getUsername();

        if (username == null || realm == null) {
            return false;
        }

        String nonce = authHeader.getNonce();
        URI uri = authHeader.getURI();
        if (uri == null) {
            return false;
        }

        String algorithm = resolveAlgorithm(authHeader);
        String A2 = request.getMethod().toUpperCase() + ":" + uri.toString();
        String HA1 = hashedPassword;
        String HA2 = SipDigestUtils.digestHex(algorithm, A2);

        String cnonce = authHeader.getCNonce();
        String KD = HA1 + ":" + nonce;
        if (cnonce != null) {
            KD += ":" + cnonce;
        }
        KD += ":" + HA2;
        String mdString = SipDigestUtils.digestHex(algorithm, KD);
        String response = authHeader.getResponse();

        return mdString.equals(response);
    }

    /**
     * Authenticate the inbound request given plain text password.
     *
     * @param request - the request to authenticate.
     * @param pass    -- the plain text password.
     * @return true if authentication succeded and false otherwise.
     */
    public static boolean doAuthenticatePlainTextPassword(Request request, String pass) {
        AuthorizationHeader authHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (authHeader == null || authHeader.getRealm() == null) {
            return false;
        }
        String realm = authHeader.getRealm().trim();
        String username = authHeader.getUsername().trim();

        if (username == null || realm == null) {
            return false;
        }

        String nonce = authHeader.getNonce();
        URI uri = authHeader.getURI();
        if (uri == null) {
            return false;
        }
        // qop 保护质量 包含auth（默认的）和auth-int（增加了报文完整性检测）两种策略
        String qop = authHeader.getQop();

        // 客户端随机数，这是一个不透明的字符串值，由客户端提供，并且客户端和服务器都会使用，以避免用明文文本。
        // 这使得双方都可以查验对方的身份，并对消息的完整性提供一些保护
        String cnonce = authHeader.getCNonce();

        // nonce计数器，是一个16进制的数值，表示同一nonce下客户端发送出请求的数量
        int nc = authHeader.getNonceCount();
        String ncStr = String.format("%08x", nc).toUpperCase();

        String algorithm = resolveAlgorithm(authHeader);
        String A1 = username + ":" + realm + ":" + pass;
        String A2 = request.getMethod().toUpperCase() + ":" + uri.toString();

        String HA1 = SipDigestUtils.digestHex(algorithm, A1);
        String HA2 = SipDigestUtils.digestHex(algorithm, A2);
        log.debug("algorithm: {}, A1: {}, A2: {}, HA1: {}, HA2: {}", algorithm, A1, A2, HA1, HA2);
        log.debug("nonce: {}, nc: {}, cnonce: {}, qop: {}", nonce, ncStr, cnonce, qop);

        String KD = HA1 + ":" + nonce;
        if (qop != null && qop.equalsIgnoreCase("auth")) {
            if (nc != -1) {
                KD += ":" + ncStr;
            }
            if (cnonce != null) {
                KD += ":" + cnonce;
            }
            KD += ":" + qop;
        }
        KD += ":" + HA2;

        String mdString = SipDigestUtils.digestHex(algorithm, KD);
        String response = authHeader.getResponse();
        log.debug("KD: {}, mdString: {}, response: {}", KD, mdString, response);
        return mdString.equals(response);
    }

    /**
     * GBT-28181-2022 §8.3：从 AuthorizationHeader 解析摘要算法名，缺失回落到 MD5。
     */
    private static String resolveAlgorithm(AuthorizationHeader authHeader) {
        String algorithm = authHeader.getAlgorithm();
        if (algorithm == null || algorithm.isBlank()) {
            return DEFAULT_ALGORITHM;
        }
        return algorithm;
    }
}
