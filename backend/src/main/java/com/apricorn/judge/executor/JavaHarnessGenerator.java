package com.apricorn.judge.executor;

import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaHarnessGenerator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateHarness(Problem problem, List<TestCase> testCases) throws Exception {
        JsonNode paramMetadata = objectMapper.readTree(problem.getParameterMetadata());

        StringBuilder sb = new StringBuilder();
        sb.append("import java.io.PrintWriter;\n");
        sb.append("import java.io.FileWriter;\n");
        sb.append("import java.util.Arrays;\n");
        sb.append("import java.util.List;\n");
        sb.append("\n");
        sb.append("public class Main {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        if (args.length == 0) return;\n");
        sb.append("        int tcIndex = Integer.parseInt(args[0]);\n");
        sb.append("        try {\n");
        sb.append("            Solution solution = new Solution();\n");
        sb.append("            switch(tcIndex) {\n");

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            JsonNode argsNode = objectMapper.readTree(tc.getInput());

            sb.append("                case ").append(i).append(": {\n");

            // Generate variable declarations for each argument
            StringBuilder methodCallArgs = new StringBuilder();
            for (int j = 0; j < paramMetadata.size(); j++) {
                JsonNode paramDef = paramMetadata.get(j);
                String type = paramDef.get("type").asText();
                String name = "arg" + j;
                JsonNode valNode = argsNode.get(j);

                sb.append("                    ").append(type).append(" ").append(name).append(" = ");
                sb.append(generateLiteral(type, valNode)).append(";\n");

                if (j > 0) methodCallArgs.append(", ");
                methodCallArgs.append(name);
            }

            // Call method
            String returnType = problem.getReturnType();
            sb.append("                    ");
            if (!"void".equals(returnType)) {
                sb.append(returnType).append(" result = ");
            }
            sb.append("solution.").append(problem.getMethodName()).append("(").append(methodCallArgs.toString()).append(");\n");

            // Write result
            if (!"void".equals(returnType)) {
                sb.append("                    writeResult(result);\n");
            } else {
                // If void, we probably need to serialize one of the arguments that was modified in place.
                // For rotate-array we changed return type to int[].
                sb.append("                    writeResult(null);\n");
            }
            
            sb.append("                    break;\n");
            sb.append("                }\n");
        }

        sb.append("            }\n");
        sb.append("        } catch (Exception e) {\n");
        sb.append("            e.printStackTrace();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        // Helper methods for serialization to result.out
        sb.append("    private static void writeResult(Object result) throws Exception {\n");
        sb.append("        try (PrintWriter pw = new PrintWriter(new FileWriter(\"result.out\"))) {\n");
        sb.append("            if (result == null) {\n");
        sb.append("                pw.print(\"null\");\n");
        sb.append("            } else if (result instanceof int[]) {\n");
        sb.append("                int[] arr = (int[]) result;\n");
        sb.append("                pw.print(\"[\");\n");
        sb.append("                for(int i=0; i<arr.length; i++) {\n");
        sb.append("                    pw.print(arr[i]);\n");
        sb.append("                    if(i < arr.length-1) pw.print(\",\");\n");
        sb.append("                }\n");
        sb.append("                pw.print(\"]\");\n");
        sb.append("            } else if (result instanceof String[]) {\n");
        sb.append("                String[] arr = (String[]) result;\n");
        sb.append("                pw.print(\"[\");\n");
        sb.append("                for(int i=0; i<arr.length; i++) {\n");
        sb.append("                    pw.print(\"\\\"\" + arr[i].replace(\"\\\"\", \"\\\\\\\"\") + \"\\\"\");\n");
        sb.append("                    if(i < arr.length-1) pw.print(\",\");\n");
        sb.append("                }\n");
        sb.append("                pw.print(\"]\");\n");
        sb.append("            } else if (result instanceof int[][]) {\n");
        sb.append("                int[][] arr = (int[][]) result;\n");
        sb.append("                pw.print(\"[\");\n");
        sb.append("                for(int i=0; i<arr.length; i++) {\n");
        sb.append("                    pw.print(\"[\");\n");
        sb.append("                    for(int j=0; j<arr[i].length; j++) {\n");
        sb.append("                        pw.print(arr[i][j]);\n");
        sb.append("                        if(j < arr[i].length-1) pw.print(\",\");\n");
        sb.append("                    }\n");
        sb.append("                    pw.print(\"]\");\n");
        sb.append("                    if(i < arr.length-1) pw.print(\",\");\n");
        sb.append("                }\n");
        sb.append("                pw.print(\"]\");\n");
        sb.append("            } else if (result instanceof String) {\n");
        sb.append("                pw.print(\"\\\"\" + result.toString().replace(\"\\\"\", \"\\\\\\\"\") + \"\\\"\");\n");
        sb.append("            } else {\n");
        sb.append("                pw.print(result.toString());\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    private String generateLiteral(String type, JsonNode node) {
        if ("int".equals(type) || "long".equals(type) || "double".equals(type) || "boolean".equals(type)) {
            return node.asText();
        } else if ("String".equals(type)) {
            return "\"" + node.asText().replace("\"", "\\\"") + "\"";
        } else if ("int[]".equals(type)) {
            StringBuilder sb = new StringBuilder("new int[]{");
            for (int i = 0; i < node.size(); i++) {
                sb.append(node.get(i).asInt());
                if (i < node.size() - 1) sb.append(", ");
            }
            sb.append("}");
            return sb.toString();
        } else if ("String[]".equals(type)) {
            StringBuilder sb = new StringBuilder("new String[]{");
            for (int i = 0; i < node.size(); i++) {
                sb.append("\"").append(node.get(i).asText().replace("\"", "\\\"")).append("\"");
                if (i < node.size() - 1) sb.append(", ");
            }
            sb.append("}");
            return sb.toString();
        } else if ("int[][]".equals(type)) {
            StringBuilder sb = new StringBuilder("new int[][]{");
            for (int i = 0; i < node.size(); i++) {
                sb.append("{");
                JsonNode inner = node.get(i);
                for (int j = 0; j < inner.size(); j++) {
                    sb.append(inner.get(j).asInt());
                    if (j < inner.size() - 1) sb.append(", ");
                }
                sb.append("}");
                if (i < node.size() - 1) sb.append(", ");
            }
            sb.append("}");
            return sb.toString();
        } else {
            throw new IllegalArgumentException("Unsupported type for literal generation: " + type);
        }
    }
}
