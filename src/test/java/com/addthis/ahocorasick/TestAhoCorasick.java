/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.ahocorasick;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TestAhoCorasick {

    @Test
    public void traverse() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("he", "he");
        tree.add("she", "she");
        tree.add("his", "his");
        tree.add("hers", "hers");
        tree.add("herhome", "herhome");
        State root = tree.getRoot();
        root.traverse(root);
    }

    @Test
    public void construction() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("hello", "hello");
        tree.add("hi", "hi");
        tree.prepare();

        State s0 = tree.getRoot();
        State s1 = s0.get('h');
        State s2 = s1.get('e');
        State s5 = s2.getFastTransitions()[3];
        State s6 = s1.get('i');

        assertEquals(s0, s1.getFail());
        assertNull(s2.getFail());
        assertEquals(s0, s2.getFastTransitions()[0]);
        assertEquals(s0, s2.getFastTransitions()[1]);
        assertEquals(s0, s2.getFastTransitions()[2]);
        assertEquals(s0, s5.getFail());
        assertEquals(s0, s6.getFail());

        assertEquals(0, s0.getOutputs().size());
        assertEquals(0, s1.getOutputs().size());
        assertEquals(0, s2.getOutputs().size());
        assertEquals(1, s5.getOutputs().size());
        assertEquals(1, s6.getOutputs().size());
    }

    @Test
    public void example() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("he", "he");
        tree.add("she", "she");
        tree.add("his", "his");
        tree.add("hers", "hers");
        assertEquals(10, tree.getRoot().size());
        tree.prepare(); // after prepare, we can't call size()
        State s0 = tree.getRoot();
        State s1 = s0.get('h');
        State s2 = s1.get('e');

        State s3 = s0.get('s');
        State s4 = s3.get('h');
        State s5 = s4.get('e');

        State s6 = s1.get('i');
        State s7 = s6.get('s');

        State s8 = s2.get('r');
        State s9 = s8.get('s');

        assertEquals(s0, s1.getFail());
        assertEquals(s0, s2.getFail());
        assertEquals(s0, s3.getFail());
        assertEquals(s0, s6.getFail());
        assertEquals(s0, s8.getFail());

        assertEquals(s1, s4.getFail());
        assertEquals(s2, s5.getFail());
        assertEquals(s3, s7.getFail());
        assertEquals(s3, s9.getFail());

        assertEquals(0, s1.getOutputs().size());
        assertEquals(0, s3.getOutputs().size());
        assertEquals(0, s4.getOutputs().size());
        assertEquals(0, s6.getOutputs().size());
        assertEquals(0, s8.getOutputs().size());
        assertEquals(1, s2.getOutputs().size());
        assertEquals(1, s7.getOutputs().size());
        assertEquals(1, s9.getOutputs().size());
        assertEquals(2, s5.getOutputs().size());
    }

    @Test
    public void startSearchWithSingleResult() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("apple", "apple");
        tree.prepare();
        SearchResult result = tree.startSearch("washington cut the apple tree");
        assertEquals(1, result.getOutputs().size());
        assertEquals("apple", result.getOutputs().iterator().next());
        assertEquals(24, result.lastIndex);
        assertEquals(null, tree.continueSearch(result));
    }

    @Test
    public void startSearchWithUnicodeResult() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("españa", "españa");
        tree.prepare();
        SearchResult result = tree.startSearch("la campeona del mundo de fútbol es españa");
        assertEquals(1, result.getOutputs().size());
        assertEquals("españa", result.getOutputs().iterator().next());
        assertEquals(41, result.lastIndex);
        assertEquals(null, tree.continueSearch(result));
    }

    @Test
    public void startSearchWithAdjacentResults() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("john", "john");
        tree.add("jane", "jane");
        tree.prepare();
        SearchResult firstResult = tree.startSearch("johnjane");
        SearchResult secondResult = tree.continueSearch(firstResult);
        assertEquals(null, tree.continueSearch(secondResult));
    }

    @Test
    public void startSearchOnEmpty() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("cipher", 0);
        tree.add("zip", 1);
        tree.add("nought", 2);
        tree.prepare();
        SearchResult result = tree.startSearch("");
        assertEquals(null, result);
    }

    @Test
    public void multipleOutputs() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("x", "x");
        tree.add("xx", "xx");
        tree.add("xxx", "xxx");
        tree.prepare();

        SearchResult result = tree.startSearch("xxx");
        assertEquals(1, result.lastIndex);
        assertEquals(new HashSet<Object>(Arrays.asList(new String[]{"x"})),
                     result.getOutputs());

        result = tree.continueSearch(result);
        assertEquals(2, result.lastIndex);
        assertEquals(
                new HashSet<Object>(Arrays.asList(new String[]{"xx", "x"})),
                result.getOutputs());

        result = tree.continueSearch(result);
        assertEquals(3, result.lastIndex);
        assertEquals(
                new HashSet<Object>(Arrays.asList(new String[]{"xxx", "xx", "x"})), result.getOutputs());

        assertEquals(null, tree.continueSearch(result));
    }

    @Test
    public void iteratorInterface() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("moo", "moo");
        tree.add("one", "one");
        tree.add("on", "on");
        tree.add("ne", "ne");
        tree.prepare();
        Iterator<SearchResult> iter = tree.progressiveSearch("one moon ago");

        assertTrue(iter.hasNext());
        SearchResult r = iter.next();
        assertEquals(new HashSet<Object>(Arrays.asList(new String[]{"on"})),
                     r.getOutputs());
        assertEquals(2, r.lastIndex);

        assertTrue(iter.hasNext());
        r = iter.next();
        assertEquals(
                new HashSet<Object>(Arrays.asList(new String[]{"one", "ne"})),
                r.getOutputs());
        assertEquals(3, r.lastIndex);

        assertTrue(iter.hasNext());
        r = iter.next();
        assertEquals(
                new HashSet<Object>(Arrays.asList(new String[]{"moo"})),
                r.getOutputs());
        assertEquals(7, r.lastIndex);

        assertTrue(iter.hasNext());
        r = iter.next();
        assertEquals(new HashSet<Object>(Arrays.asList(new String[]{"on"})),
                     r.getOutputs());
        assertEquals(8, r.lastIndex);

        assertFalse(iter.hasNext());

        try {
            iter.next();
            fail();
        } catch (NoSuchElementException e) {
        }

    }

    @Test
    public void largerTextExample() {
        AhoCorasick tree = AhoCorasick.builder().build();
        String text =
                "The ga3 mutant of Arabidopsis is a gibberellin-responsive dwarf. We present data showing that the " +
                "ga3-1 mutant is deficient in ent-kaurene oxidase activity, the first cytochrome P450-mediated step " +
                "in" +
                " the gibberellin biosynthetic pathway. By using a combination of conventional map-based cloning and " +
                "random sequencing we identified a putative cytochrome P450 gene mapping to the same location as GA3." +
                " " +
                "Relative to the progenitor line, two ga3 mutant alleles contained single base changes generating " +
                "in-frame stop codons in the predicted amino acid sequence of the P450. A genomic clone spanning the " +
                "P450 locus complemented the ga3-2 mutant. The deduced GA3 protein defines an additional class of " +
                "cytochrome P450 enzymes. The GA3 gene was expressed in all tissues examined, RNA abundance being " +
                "highest in inflorescence tissue.";
        String[] terms = {"microsome", "cytochrome",
                          "cytochrome P450 activity", "gibberellic acid biosynthesis",
                          "GA3", "cytochrome P450", "oxygen binding", "AT5G25900.1",
                          "protein", "RNA", "gibberellin", "Arabidopsis",
                          "ent-kaurene oxidase activity", "inflorescence", "tissue",};
        for (int i = 0; i < terms.length; i++) {
            tree.add(terms[i], terms[i]);
        }
        tree.prepare();

        Set<Object> termsThatHit = new HashSet<Object>();
        Iterator<SearchResult> iter = tree.progressiveSearch(text);
        while (iter.hasNext()) {
            SearchResult result = iter.next();
            termsThatHit.addAll(result.getOutputs());
        }
        assertEquals(
                new HashSet<Object>(Arrays.asList(new String[]{"cytochrome",
                                                               "GA3", "cytochrome P450", "protein", "RNA",
                                                               "gibberellin", "Arabidopsis",
                                                               "ent-kaurene oxidase activity", "inflorescence",
                                                               "tissue",})), termsThatHit);

    }

    // Without overlapping
    @Test
    public void removeOverlapping1() {
        AhoCorasick tree = AhoCorasick.builder().build();
        List<OutputResult> outputResults = new ArrayList<OutputResult>();
        outputResults.add(new OutputResult(0, 0, 2));
        outputResults.add(new OutputResult(1, 2, 4));
        outputResults.add(new OutputResult(2, 5, 6));

        assertEquals(3, outputResults.size());
        tree.removeOverlapping(outputResults); // No effect
        assertEquals(3, outputResults.size());
    }

    // With a clear overlapping
    @Test
    public void removeOverlapping2() {
        AhoCorasick tree = AhoCorasick.builder().build();
        List<OutputResult> outputResults = new ArrayList<OutputResult>();
        outputResults.add(new OutputResult(0, 0, 2));
        outputResults.add(new OutputResult(1, 1, 4));
        outputResults.add(new OutputResult(2, 5, 6));

        assertEquals(3, outputResults.size());
        tree.removeOverlapping(outputResults);
        assertEquals(2, outputResults.size());
        assertEquals(0, outputResults.get(0).getOutput());
        assertEquals(2, outputResults.get(1).getOutput());
    }

    // With two overlapping, one with the same start index
    @Test
    public void removeOverlapping3() {
        AhoCorasick tree = AhoCorasick.builder().build();
        List<OutputResult> outputResults = new ArrayList<OutputResult>();
        outputResults.add(new OutputResult(0, 0, 2));
        outputResults.add(new OutputResult(1, 0, 4));
        outputResults.add(new OutputResult(2, 3, 6));

        assertEquals(3, outputResults.size());
        tree.removeOverlapping(outputResults);
        assertEquals(1, outputResults.size());
        assertEquals(1, outputResults.get(0).getOutput());
    }

    @Test
    public void completeSearchNotOverlapping() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("Apple");
        tree.add("App");
        tree.add("Microsoft");
        tree.add("Mic");
        tree.prepare();

        String inputText = "Apple is better than Microsoft";
        List<OutputResult> results = tree.completeSearch(inputText, false, false);

        assertEquals(2, results.size());
        assertEquals("Apple", results.get(0).getOutput());
        assertEquals("Microsoft", results.get(1).getOutput());
    }

    @Test
    public void completeSearchOverlapping() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("Apple");
        tree.add("App");
        tree.add("Microsoft");
        tree.add("Mic");
        tree.prepare();

        String inputText = "Apple is better than Microsoft";
        List<OutputResult> results = tree.completeSearch(inputText, true, false);

        assertEquals(4, results.size());
        assertEquals("App", results.get(0).getOutput());
        assertEquals("Apple", results.get(1).getOutput());
        assertEquals("Mic", results.get(2).getOutput());
        assertEquals("Microsoft", results.get(3).getOutput());
    }

    @Test
    public void completeSearchTokenized1() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("Apple");
        tree.add("e i");
        tree.add("than Microsoft");
        tree.add("Microsoft");
        tree.add("er than");
        tree.prepare();

        String inputText = "Apple is better than Microsoft";
        List<OutputResult> results = tree.completeSearch(inputText, false, true); // without overlapping

        assertEquals(2, results.size());
        assertEquals("Apple", results.get(0).getOutput());
        assertEquals("than Microsoft", results.get(1).getOutput());

        results = tree.completeSearch(inputText, true, true); // with overlapping

        assertEquals(3, results.size());
        assertEquals("Apple", results.get(0).getOutput());
        assertEquals("than Microsoft", results.get(1).getOutput());
        assertEquals("Microsoft", results.get(2).getOutput());
    }

    @Test
    public void completeSearchTokenized2() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("Real Madrid");
        tree.add("Madrid");
        tree.add("Barcelona");
        tree.add("Messi");
        tree.add("esp");
        tree.add("o p");
        tree.add("Mes");
        tree.add("Rea");
        tree.prepare();

        String inputText = "El Real Madrid no puede fichar a Messi porque es del Barcelona";
        List<OutputResult> results = tree.completeSearch(inputText, false, true); // without overlapping

        assertEquals(3, results.size());
        assertEquals("Real Madrid", results.get(0).getOutput());
        assertEquals("Messi", results.get(1).getOutput());
        assertEquals("Barcelona", results.get(2).getOutput());
    }

    @Test
    public void completeSearchTokenized3() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("comp");
        tree.prepare();

        String inputText = "    A    complete      sentence     ";
        List<OutputResult> results = tree.completeSearch(inputText, false, true); // without overlapping

        assertEquals(0, results.size());
    }

    @Test
    public void completeSearchTokenized4() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("Madrid");
        tree.add("Real");
        tree.add("Real Madrid");
        tree.add("El Real de España");
        tree.prepare();

        String inputText = "El Real Madrid no puede fichar a Messi porque es del Barcelona";
        List<OutputResult> results = tree.completeSearch(inputText, false, true); // without overlapping

        assertEquals(1, results.size());
        assertEquals("Real Madrid", results.get(0).getOutput());
    }

    @Test
    public void completeSearchTokenized5() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("Microsoft");
        tree.add("than Microsoft");
        tree.add("han Microsoft");
        tree.add("n Microsoft");
        tree.add(" Microsoft");
        tree.prepare();

        String inputText = "Apple is better than Microsoft";
        List<OutputResult> results = tree.completeSearch(inputText, true, true); // with overlapping

        assertEquals(2, results.size());
        assertEquals("than Microsoft", results.get(0).getOutput());
        assertEquals("Microsoft", results.get(1).getOutput());
    }

    @Test
    public void pathCompression() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("hello");
        tree.add("world");
        tree.prepare();

        State root = tree.getRoot();
        State hState = root.get('h');
        State wState = root.get('w');
        assertEquals("ello", hState.getFastPath());
        assertEquals("orld", wState.getFastPath());

        String inputText = "helloworl";
        List<OutputResult> results = tree.completeSearch(inputText, false, false);
        assertEquals(1, results.size());
        inputText = "helloworld";
        results = tree.completeSearch(inputText, false, false);
        assertEquals(2, results.size());
    }

    @Test
    public void fastpathEarlyTermination() {
        AhoCorasick tree = AhoCorasick.builder().build();
        tree.add("abcdefg");
        tree.prepare();
        List<OutputResult> results = tree.completeSearch("abcde", true, false);
        assertEquals(0, results.size());
    }

    private static final int MIN_LENGTH = 3;

    private static final int MAX_LENGTH = 20;

    private void randomOneIteration(int iter) {
        Random random = new Random();
        Set<String> elements = new LinkedHashSet<>();
        AhoCorasick tree = AhoCorasick.builder().build();
        for (int i = 0; i < 10; i++) {
            int length = random.nextInt(MAX_LENGTH - MIN_LENGTH) + MIN_LENGTH;
            String candidate = RandomStringUtils.random(length, 'a', 'b', 'c');
            elements.add(candidate);
            tree.add(candidate);
        }
        tree.prepare();
        StringBuilder builder = new StringBuilder();
        for (String element : elements) {
            builder.append(element);
        }
        String input = builder.toString();
        List<OutputResult> results = tree.completeSearch(input, true, false);
        Set<String> output = new HashSet<>();
        for (OutputResult outputResult : results) {
            output.add(outputResult.getOutput().toString());
        }
        if (elements.size() > output.size()) {
            Set<String> extra = new LinkedHashSet<>(elements);
            extra.removeAll(output);
            fail("At iteration " + iter + " did not find the elements: " + extra + " out of the input set: " +
                 elements);
        } else if (elements.size() < output.size()) {
            output.removeAll(elements);
            fail("At iteration " + iter + " + found extra elements: " + output);
        }
    }

    @Test
    public void randomIterations() {
        for (int i = 0; i < 1000; i++) {
            randomOneIteration(i);
        }
    }
}
