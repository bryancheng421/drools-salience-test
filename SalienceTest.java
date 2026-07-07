///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.drools:drools-mvel:8.44.0.Final
//DEPS org.kie:kie-api:8.44.0.Final

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SalienceTest {
    public static void main(String[] args) throws Exception {
        String drl = new String(Files.readAllBytes(Paths.get("rules.drl")));

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/rules/rules.drl", drl);

        KieBuilder builder = ks.newKieBuilder(kfs);
        builder.buildAll();
        if (builder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            System.out.println(builder.getResults().toString());
            return;
        }

        KieContainer kc = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());

        int aFirstCount = 0;
        int bFirstCount = 0;

        for (int i = 1; i <= 100; i++) {
            KieSession session = kc.newKieSession();
            List<String> firedOrder = new ArrayList<>();
            session.setGlobal("firedOrder", firedOrder);

            session.insert("go");
            session.fireAllRules();
            session.dispose();

            String firstFired = firedOrder.get(0);
            if (firstFired.equals("A")) {
                aFirstCount++;
            } else {
                bFirstCount++;
            }

            System.out.println("Run " + i + ": order = " + firedOrder + " -> first fired: " + firstFired);
        }

        System.out.println();
        System.out.println("=== Summary over 100 runs ===");
        System.out.println("Rule A fired first: " + aFirstCount + " times");
        System.out.println("Rule B fired first: " + bFirstCount + " times");
    }
}
