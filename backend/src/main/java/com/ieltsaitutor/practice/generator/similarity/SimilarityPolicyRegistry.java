package com.ieltsaitutor.practice.generator.similarity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SimilarityPolicyRegistry {

    private final Map<String, SimilarityPolicy> policies = new ConcurrentHashMap<>();
    private volatile String activePolicyVersion = "similarity-policy-v1.0-conservative";

    public SimilarityPolicyRegistry() {
        register(SimilarityPolicy.conservativeV1());
    }

    public void register(SimilarityPolicy policy) {
        policies.put(policy.version(), policy);
    }

    public SimilarityPolicy getActivePolicy() {
        return policies.getOrDefault(activePolicyVersion, SimilarityPolicy.conservativeV1());
    }

    public void setActivePolicyVersion(String version) {
        if (policies.containsKey(version)) {
            this.activePolicyVersion = version;
        }
    }
}
