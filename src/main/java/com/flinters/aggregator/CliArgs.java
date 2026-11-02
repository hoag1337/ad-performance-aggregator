package com.flinters.aggregator;

/** Parsed and validated command-line arguments. */
record CliArgs(String input, String output) {

    static final String USAGE = "Usage: aggregator --input <ad_data.csv> --output <results-dir>";

    /**
     * @throws IllegalArgumentException if required flags are missing or unrecognized flags are given
     */
    static CliArgs parse(String[] args) {
        String input = null;
        String output = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input" -> input = requireValue(args, ++i, "--input");
                case "--output" -> output = requireValue(args, ++i, "--output");
                default -> throw new IllegalArgumentException("Unrecognized argument: " + args[i]);
            }
        }

        if (input == null || output == null) {
            throw new IllegalArgumentException("Both --input and --output are required.");
        }
        return new CliArgs(input, output);
    }

    private static String requireValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + flag);
        }
        return args[index];
    }
}
