package com.ieltsaitutor.learning.intelligence;

public interface MistakeClassifier {
    MistakeClassification classify(MistakeEvidence evidence);
}
