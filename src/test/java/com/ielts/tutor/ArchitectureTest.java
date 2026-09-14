package com.ielts.tutor;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.ielts.tutor", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    // R1. presentation chỉ depend vào application, shared, java.*
    @ArchTest
    public static final ArchRule r1_presentation_dependencies = classes()
            .that().resideInAPackage("..presentation..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..presentation..",
                    "..application..",
                    "..shared..",
                    "java..",
                    "org.springframework.."
            ).as("R1: presentation chỉ depend vào application, shared, java.*");

    // R2. domain chỉ depend vào domain, ai.api, infrastructure, shared, java.*
    @ArchTest
    public static final ArchRule r2_domain_dependencies = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..domain..",
                    "..ai.api..",
                    "..infrastructure..",
                    "..shared..",
                    "java..",
                    "org.springframework.."
            ).as("R2: domain chỉ depend vào domain, ai.api, infrastructure, shared, java.*");

    // R3. shared chỉ depend vào shared, java.*
    @ArchTest
    public static final ArchRule r3_shared_dependencies = classes()
            .that().resideInAPackage("..shared..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..shared..",
                    "java..",
                    "org.springframework.."
            ).as("R3: shared chỉ depend vào shared, java.*");

    // R4. ai chỉ depend vào ai, infrastructure, shared, java.*
    @ArchTest
    public static final ArchRule r4_ai_dependencies = classes()
            .that().resideInAPackage("..ai..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..ai..",
                    "..infrastructure..",
                    "..shared..",
                    "java..",
                    "org.springframework.."
            ).as("R4: ai chỉ depend vào ai, infrastructure, shared, java.*");

    // R5. domain KHÔNG được depend vào ai.asr, ai.nlp, ai.tts, ai.recommendation, ai.chatbot (chỉ ai.api)
    @ArchTest
    public static final ArchRule r5_domain_no_ai_internal_dependencies = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..ai.asr..",
                    "..ai.nlp..",
                    "..ai.tts..",
                    "..ai.recommendation..",
                    "..ai.chatbot.."
            ).as("R5: domain KHÔNG được depend vào ai.asr, ai.nlp, ai.tts, ai.recommendation, ai.chatbot (chỉ ai.api)");

    // R6. Không có vòng phụ thuộc giữa các module (slices matching "com.ielts.tutor.(*).." beFreeOfCycles)
    @ArchTest
    public static final ArchRule r6_slices_free_of_cycles = slices()
            .matching("com.ielts.tutor.(*)..")
            .should().beFreeOfCycles()
            .as("R6: Không có vòng phụ thuộc giữa các module");

    // R7. infrastructure chỉ depend vào infrastructure, shared, java.*
    @ArchTest
    public static final ArchRule r7_infrastructure_dependencies = classes()
            .that().resideInAPackage("..infrastructure..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..infrastructure..",
                    "..shared..",
                    "java..",
                    "org.springframework.."
            ).as("R7: infrastructure chỉ depend vào infrastructure, shared, java.*");
}
