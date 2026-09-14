package com.ielts.tutor.tools;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class DependencyGraphExtractor {

    private static final String BASE_PACKAGE_PREFIX = "com.ielts.tutor.";
    private static final Set<String> IGNORED_PREFIXES = Set.of(
            "java.", "javax.", "jakarta.", "org.springframework."
    );

    public static void main(String[] args) throws IOException {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        Path scanRoot = projectRoot.resolve("src/main/java/com/ielts/tutor");

        if (!Files.exists(scanRoot)) {
            System.err.println("Error: Directory not found: " + scanRoot);
            System.exit(1);
        }

        List<Path> javaFiles;
        try (Stream<Path> stream = Files.walk(scanRoot)) {
            javaFiles = stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        int totalFiles = javaFiles.size();
        Set<String> allModules = new TreeSet<>();
        Map<String, Integer> edgeWeights = new TreeMap<>();
        List<List<String>> edgeList = new ArrayList<>();

        // First pass: discover all modules
        for (Path file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                Optional<PackageDeclaration> pkgOpt = cu.getPackageDeclaration();
                if (pkgOpt.isPresent()) {
                    String pkgName = pkgOpt.get().getNameAsString();
                    String module = extractModule(pkgName);
                    if (module != null && !module.equals("tools")) {
                        allModules.add(module);
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to parse " + file + ": " + e.getMessage());
            }
        }

        // Second pass: extract dependencies
        for (Path file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                Optional<PackageDeclaration> pkgOpt = cu.getPackageDeclaration();
                if (pkgOpt.isEmpty()) {
                    continue;
                }
                String srcModule = extractModule(pkgOpt.get().getNameAsString());
                if (srcModule == null || srcModule.equals("tools")) {
                    continue;
                }

                Set<String> importedFqns = new HashSet<>();

                // 1. Import statements
                for (ImportDeclaration imp : cu.getImports()) {
                    String impName = imp.getNameAsString();
                    importedFqns.add(impName);
                    processDependencyCandidate(srcModule, impName, edgeWeights);
                }

                // 2. Fully-qualified names in class body
                cu.findAll(ClassOrInterfaceType.class).forEach(type -> {
                    String name = type.asString();
                    if (name.startsWith(BASE_PACKAGE_PREFIX) && !importedFqns.contains(name)) {
                        processDependencyCandidate(srcModule, name, edgeWeights);
                    }
                });

                cu.findAll(FieldAccessExpr.class).forEach(fae -> {
                    String name = fae.toString();
                    if (name.startsWith(BASE_PACKAGE_PREFIX) && !importedFqns.contains(name)) {
                        processDependencyCandidate(srcModule, name, edgeWeights);
                    }
                });

            } catch (Exception e) {
                System.err.println("Warning: Failed to parse " + file + ": " + e.getMessage());
            }
        }

        // Build edges list
        for (String edgeKey : edgeWeights.keySet()) {
            String[] parts = edgeKey.split("->");
            edgeList.add(List.of(parts[0], parts[1]));
        }

        // Write graph.json at project root
        Path graphJsonPath = projectRoot.resolve("graph.json");
        String jsonContent = buildJson(allModules, edgeList, edgeWeights, totalFiles);
        Files.writeString(graphJsonPath, jsonContent);

        // Print stdout
        System.out.println("Số file quét: " + totalFiles);
        System.out.println("Số cạnh tìm được: " + edgeWeights.size());
        System.out.println("Danh sách cạnh + trọng số:");
        edgeWeights.forEach((edge, weight) -> {
            System.out.printf("  %s: %d%n", edge, weight);
        });
        System.out.println("Đường dẫn graph.json: " + graphJsonPath.toAbsolutePath());
    }

    private static String extractModule(String pkgName) {
        if (!pkgName.startsWith(BASE_PACKAGE_PREFIX)) {
            return null;
        }
        String sub = pkgName.substring(BASE_PACKAGE_PREFIX.length());
        return sub.contains(".") ? sub.substring(0, sub.indexOf('.')) : sub;
    }

    private static void processDependencyCandidate(String srcModule,
                                                   String candidateName,
                                                   Map<String, Integer> edgeWeights) {
        for (String ignored : IGNORED_PREFIXES) {
            if (candidateName.startsWith(ignored)) {
                return;
            }
        }

        if (!candidateName.startsWith(BASE_PACKAGE_PREFIX)) {
            return;
        }

        String targetModule = extractModule(candidateName);
        if (targetModule == null || targetModule.equals("tools")) {
            return;
        }

        // Bỏ self-loop
        if (targetModule.equals(srcModule)) {
            return;
        }

        String edgeKey = srcModule + "->" + targetModule;
        edgeWeights.put(edgeKey, edgeWeights.getOrDefault(edgeKey, 0) + 1);
    }

    private static String buildJson(Set<String> nodes,
                                    List<List<String>> edges,
                                    Map<String, Integer> weights,
                                    int totalFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // nodes
        sb.append("  \"nodes\": [\n");
        int nodeIndex = 0;
        for (String node : nodes) {
            sb.append("    \"").append(node).append("\"");
            if (++nodeIndex < nodes.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ],\n");

        // edges
        sb.append("  \"edges\": [\n");
        for (int i = 0; i < edges.size(); i++) {
            List<String> edge = edges.get(i);
            sb.append("    [\"").append(edge.get(0)).append("\", \"").append(edge.get(1)).append("\"]");
            if (i < edges.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ],\n");

        // weights
        sb.append("  \"weights\": {\n");
        int weightIndex = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            sb.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            if (++weightIndex < weights.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  },\n");

        // stats
        sb.append("  \"stats\": {\n");
        sb.append("    \"total_files\": ").append(totalFiles).append(",\n");
        sb.append("    \"total_edges\": ").append(edges.size()).append(",\n");
        sb.append("    \"total_modules\": ").append(nodes.size()).append("\n");
        sb.append("  }\n");

        sb.append("}\n");
        return sb.toString();
    }
}
