package com.jk.explore.security.leastprivilegetf;

import java.util.ArrayList;
import java.util.List;

/** The fixed list of (action, resource) pairs the online store's account offers. The blast radius of an identity is how many it may use. */
final class Catalogue {

    record Pair(String action, String resource) {
    }

    static final String REPORTS = "arn:aws:s3:::shop-reports";
    static final String REPORT_FILE = "arn:aws:s3:::shop-reports/q3.csv";
    static final String SECRETS = "arn:aws:s3:::shop-secrets";
    static final String SECRET_FILE = "arn:aws:s3:::shop-secrets/keys.pem";
    static final String ORDERS = "arn:aws:dynamodb:eu-west-1:000000000000:table/orders";
    static final String CUSTOMERS = "arn:aws:dynamodb:eu-west-1:000000000000:table/customers";
    static final String NEW_USER = "arn:aws:iam::000000000000:user/new";
    static final String DEPLOY_ROLE = "arn:aws:iam::000000000000:role/deploy";
    static final String KEY = "arn:aws:kms:eu-west-1:000000000000:key/shop";
    static final String CHECKOUT = "arn:aws:lambda:eu-west-1:000000000000:function:checkout";

    private Catalogue() {
    }

    static List<Pair> all() {
        List<Pair> pairs = new ArrayList<>();
        pairs.add(new Pair("s3:ListBucket", REPORTS));
        pairs.add(new Pair("s3:ListBucket", SECRETS));
        for (String action : List.of("s3:GetObject", "s3:PutObject", "s3:DeleteObject")) {
            pairs.add(new Pair(action, REPORT_FILE));
            pairs.add(new Pair(action, SECRET_FILE));
        }
        for (String action : List.of("dynamodb:GetItem", "dynamodb:Query", "dynamodb:Scan", "dynamodb:PutItem", "dynamodb:DeleteItem")) {
            pairs.add(new Pair(action, ORDERS));
            pairs.add(new Pair(action, CUSTOMERS));
        }
        pairs.add(new Pair("iam:CreateUser", NEW_USER));
        pairs.add(new Pair("iam:AttachUserPolicy", NEW_USER));
        pairs.add(new Pair("iam:PassRole", DEPLOY_ROLE));
        pairs.add(new Pair("sts:AssumeRole", DEPLOY_ROLE));
        pairs.add(new Pair("kms:Decrypt", KEY));
        pairs.add(new Pair("lambda:CreateFunction", CHECKOUT));
        pairs.add(new Pair("lambda:InvokeFunction", CHECKOUT));
        return List.copyOf(pairs);
    }

    static List<Pair> reachable(Evaluator evaluator) {
        return all().stream().filter(p -> evaluator.allows(p.action(), p.resource())).toList();
    }

    static int blastRadius(Evaluator evaluator) {
        return reachable(evaluator).size();
    }
}
