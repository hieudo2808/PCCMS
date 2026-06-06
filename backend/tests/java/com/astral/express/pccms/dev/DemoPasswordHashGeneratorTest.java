package com.astral.express.pccms.dev;

import com.astral.express.pccms.identity.security.PepperBCryptEncoder;
import org.junit.jupiter.api.Test;

/**
 * Chạy một lần để sinh hash cho seed: mvn test -Dtest=DemoPasswordHashGeneratorTest
 */
class DemoPasswordHashGeneratorTest {

    private static final String PEPPER = "local-dev-pepper-change-me";

    @Test
    void printDemoPasswordHashes() {
        PepperBCryptEncoder encoder = new PepperBCryptEncoder(12, PEPPER);
        String[][] accounts = {
                {"admin@pccms.vn", "admin123"},
                {"staff.le@pccms.vn", "staff123"},
                {"owner@pccms.vn", "owner123"},
                {"vet.an@pccms.vn", "vet123"},
                {"vet.huong@pccms.vn", "vet123"},
        };
        for (String[] account : accounts) {
            System.out.println(account[0] + " / " + account[1]);
            System.out.println(encoder.encode(account[1]));
            System.out.println();
        }
    }
}
