package com.coen390.j_abba.random_facts;

import java.util.Random;

public class FactsModel {
    static private String[] crazy_facts_array = {"test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test ","crazy_fact1", "crazy_fact2", "crazy_fact3",
            "crazy_fact4", "crazy_fact5", "crazy_fact6",
            "crazy_fact7", "crazy_fact8", "crazy_fact9",
            "crazy_fact10", "crazy_fact11", "crazy_fact12",
            "crazy_fact13", "crazy_fact14", "crazy_fact15"};


    static private String[] sports_facts_array = {"sports_fact1", "sports_fact2", "sports_fact3",
            "sports_fact4", "sports_fact5", "sports_fact6",
            "sports_fact7", "sports_fact8", "sports_fact9",
            "sports_fact10", "sports_fact11", "sports_fact12",
            "sports_fact13", "sports_fact14", "sports_fact15"};

    static public String pickRandomFact (String[] array_of_facts) {
        int max_index = array_of_facts.length;
        Random rand = new Random();
        int rand_index = rand.nextInt(max_index);
        return array_of_facts[rand_index];
    };

    static public String factSpitter (String type) {
        String fact = "";
        switch (type) {
            case "sports":
                fact = pickRandomFact(sports_facts_array);
            case "crazy":
                fact = pickRandomFact(crazy_facts_array);
        }
        return fact;
    };


}
