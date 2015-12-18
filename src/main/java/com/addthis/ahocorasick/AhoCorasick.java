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

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import com.google.common.base.Preconditions;

import com.gs.collections.api.list.primitive.MutableIntList;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.TokenizerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An implementation of the Aho-Corasick string searching automaton. This
 * implementation of the <a
 * href="http://portal.acm.org/citation.cfm?id=360855&dl=ACM&coll=GUIDE"
 * target="_blank">Aho-Corasick</a> algorithm is optimized to work with chars.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 *     AhoCorasick tree = new AhoCorasick();
 *     tree.add("hello");
 *     tree.add("world");
 *     tree.prepare();
 *
 *     Iterator searcher = tree.progressiveSearch("hello world");
 *     while (searcher.hasNext()) {
 *         SearchResult result = searcher.next();
 *         System.out.println(result.getOutputs());
 *         System.out.println("Found at index: " + result.getEndIndex());
 *     }
 * </pre>
 */
public class AhoCorasick {

    private static final Logger log = LoggerFactory.getLogger(AhoCorasick.class);

    private final State root;
    private final OutputSizeCalculator outputSizeCalculator;
    private final TokenizerFactory tokenizerFactory;
    private boolean prepared;

    private AhoCorasick(OutputSizeCalculator outputSizeCalculator, TokenizerFactory tokenizerFactory) {
        Preconditions.checkNotNull(outputSizeCalculator, "The outputSizeCalculator parameter must be non-null");
        this.root = new State(0);
        this.outputSizeCalculator = outputSizeCalculator;
        this.tokenizerFactory = tokenizerFactory;
        this.prepared = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private OutputSizeCalculator outputSizeCalculator = new StringOutputSizeCalculator();
        private TokenizerFactory tokenizerFactory = new StandardTokenizerFactory(new HashMap<>());

        public Builder setOutputSizeCalculator(OutputSizeCalculator calculator) {
            outputSizeCalculator = calculator;
            return this;
        }

        public Builder setTokenizerFactory(TokenizerFactory factory) {
            tokenizerFactory = factory;
            return this;
        }

        public AhoCorasick build() {
            return new AhoCorasick(outputSizeCalculator, tokenizerFactory);
        }
    }

    /**
     * Adds a new keyword with the associated String as output. During search,
     * if the keyword is matched, output will be one of the yielded elements in
     * SearchResults.getOutputs().
     *
     * @param keyword new keyword to include in automaton
     * @see AhoCorasick#add(String, Object)
     */
    public void add(String keyword) {
        add(keyword, keyword);
    }

    /**
     * Adds a new keyword with the given output. During search, if the keyword
     * is matched, output will be one of the yielded elements in
     * SearchResults.getOutputs().
     *
     * @param keyword new keyword to include in automaton
     * @param output  object to associate with the keyword
     */
    public void add(String keyword, Object output) {
        if (prepared) {
            throw new IllegalStateException(
                    "can't add keywords after prepare() is called");
        }
        State lastState = root.extendAll(keyword);
        lastState.addOutput(output);
    }

    /**
     * Prepares the automaton for searching. This must be called before any
     * searching().
     */
    public void prepare() {
        prepareFailTransitions();
        root.compressPaths();
        prepared = true;
    }

    /**
     * Starts a new search, and returns an Iterator of SearchResults.
     *
     * @param input (non-null) string to perform matches against
     * @return iterator of search results
     */
    public Iterator<SearchResult> progressiveSearch(String input) {
        return new Searcher(this, startSearch(input));
    }


    /**
     * Perform a search over the input text and return all the OutputResult objects,
     * ordered by position.
     *
     * @param input            (non-null) string to perform matches against
     * @param allowOverlapping if true then return results that overlap
     * @param onlyTokens       if true then apply tokenizer to returns
     * @return list of matching results
     */
    public List<OutputResult> completeSearch(String input,
                                             boolean allowOverlapping, boolean onlyTokens) {
        return completeSearch(input, allowOverlapping, onlyTokens, null);
    }

    /**
     * Perform a search over the input text and return all the OutputResult objects,
     * ordered by position.
     *
     * @param input            (non-null) string to perform matches against
     * @param allowOverlapping if true then return results that overlap
     * @param onlyTokens       if true then apply tokenizer to returns
     * @param tokenizer        optionally provide the tokenizer. Useful for recycling tokenizer objects.
     * @return list of matching results
     */
    public List<OutputResult> completeSearch(String input,
                                             boolean allowOverlapping, boolean onlyTokens, Tokenizer tokenizer) {
        List<OutputResult> result;
        Searcher searcher = new Searcher(this, startSearch(input));

        // Recollection the valid outputs
        result = recollectOutputResults(searcher, input, onlyTokens, tokenizer);

        // Sorting the result list according to the startIndex of each output
        sortOutputResults(result);

        // Removing overlappings
        if (!allowOverlapping) {
            removeOverlapping(result);
        }

        return result;
    }

    /**
     * The non-overlapping outputs are taken to be the left-most and
     * longest-matching, according to the following definitions. An output with
     * span <code>(start1,last1)</code> overlaps an output with span
     * <code>(start2,last2)</code> if and only if either end points of the
     * second output lie within the first chunk:
     * <p>
     * <code>start1 &lt;= start2 &lt; last1</code>, or
     * <code>start1 &lt; last2 &lt;= last1</code>.
     * <p>
     * For instance, <code>(0,1)</code> and <code>(1,3)</code> do not overlap,
     * but <code>(0,1)</code> overlaps <code>(0,2)</code>, <code>(1,2)</code>
     * overlaps <code>(0,2)</code>, and <code>(1,7)</code> overlaps
     * <code>(2,3)</code>.
     * <p>
     * <p>
     * An output <code>output1=(start1,last1)</code> dominates another output
     * <code>output2=(start2,last2)</code> if and only if the outputs overlap
     * and:
     * <p>
     * <code>start1 &lt; start2</code> (leftmost), or
     * <code>start1 == start2</code> and <code>last1 &gt; last2</code>
     * (longest).
     */
    void removeOverlapping(List<OutputResult> outputResults) {
        int currentIndex = 0;
        OutputResult current, next;

        while (currentIndex < (outputResults.size() - 1)) {
            current = outputResults.get(currentIndex);
            // We will check the current output with the next one
            next = outputResults.get(currentIndex + 1);

            if (!current.isOverlapped(next)) {
                // without overlapping we can advance without problems
                currentIndex++;
            } else if (current.dominate(next)) {
                // the current one dominates the next one -> we remove the next
                // one
                outputResults.remove(currentIndex + 1);
            } else {
                // the next one dominates the current one -> we remove the
                // current one
                outputResults.remove(currentIndex);
            }
        }
    }

    /**
     * DANGER DANGER: dense algorithm code ahead. Very order dependent.
     * Initializes the fail transitions of all states except for the root.
     */
    private void prepareFailTransitions() {
        Queue<State> q = new ArrayDeque<>();
        char[] keys = root.keys();

        for (char key : keys) {
            State state = root.get(key);
            state.setFail(root);
            q.add(state);
        }

        while (!q.isEmpty()) {
            State state = q.remove();
            keys = state.keys();
            for (char key : keys) {
                State next = state.get(key);
                q.add(next);
                State fail = state.getFail();
                State failTransition = fail.get(key);
                while (failTransition == null) {
                    fail = fail.getFail();
                    failTransition = fail.get(key);
                }
                next.setFail(failTransition);
                next.addOutputs(failTransition.getOutputs());
            }
        }
    }

    /**
     * Returns the root of the tree.
     */
    State getRoot() {
        return root;
    }

    /**
     * Begins a new search using the raw interface.
     */
    SearchResult startSearch(String chars) {
        if (!prepared) {
            throw new IllegalStateException(
                    "can't start search until prepare()");
        }

        return continueSearch(new SearchResult(root, chars, 0));
    }

    /**
     * Continues the search, given the initial state described by the lastResult.
     */
    SearchResult continueSearch(SearchResult lastResult) {
        String chars = lastResult.chars;
        State state = lastResult.lastMatchedState;
        MutableInt currentIndex = new MutableInt(lastResult.lastIndex);

        while (currentIndex.getValue() < chars.length()) {

            state = state.next(chars, currentIndex);

            if (state.getOutputs().size() > 0) {
                // We have reached a node with outputs -> a new result
                return new SearchResult(state, chars, currentIndex.getValue());
            }
        }
        return null;
    }

    private TokensInformation extractTokensInformation(String chars, Tokenizer tokenizer) {
        TokensInformation result = new TokensInformation();
        MutableIntList starts = new IntArrayList();
        MutableIntList ends = new IntArrayList();
        tokenizer = configureTokenizer(chars, tokenizer);
        OffsetAttribute offsetAttribute = tokenizer.addAttribute(OffsetAttribute.class);
        try {
            tokenizer.reset();
            while (tokenizer.incrementToken()) {
                starts.add(offsetAttribute.startOffset());
                ends.add(offsetAttribute.endOffset());
            }
            tokenizer.end();
            tokenizer.close();
        } catch (Exception ex) {
            log.error("Error tokenizing the input text: ", ex);
        }

        result.setEnds(ends);
        result.setStarts(starts);

        return result;
    }

    private Tokenizer configureTokenizer(String chars, Tokenizer tokenizer) {
        try {
            if (tokenizer == null) {
                tokenizer = tokenizerFactory.create();
            }
            tokenizer.reset();
            tokenizer.setReader(new StringReader(chars));
            return tokenizer;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static final Comparator<OutputResult> OUTPUT_RESULTS_COMPARATOR = new Comparator<OutputResult>() {
        @Override
        public int compare(OutputResult o1, OutputResult o2) {
            return o1.getStartIndex() - o2.getStartIndex();
        }
    };

    private void sortOutputResults(List<OutputResult> outputResults) {
        Collections.sort(outputResults, OUTPUT_RESULTS_COMPARATOR);
    }

    private List<OutputResult> recollectOutputResults(Searcher searcher,
                                                      String chars, boolean onlyTokens, Tokenizer tokenizer) {
        int startIndex;
        SearchResult searchResult;
        TokensInformation tokensInformation = null;
        List<OutputResult> result = new ArrayList<OutputResult>();

        if (searcher.hasNext() && onlyTokens) {
            tokensInformation = extractTokensInformation(chars, tokenizer);
        }

        // Iteration over the results
        while (searcher.hasNext()) {
            searchResult = searcher.next();

            // Iterating over the outputs
            for (Object output : searchResult.getOutputs()) {
                startIndex = searchResult.lastIndex
                             - outputSizeCalculator.calculateSize(output);
                if (!onlyTokens || tokensInformation.areValidOffsets(startIndex, searchResult.lastIndex)) {
                    result.add(new OutputResult(output, startIndex, searchResult.lastIndex));
                }
            }
        }

        return result;
    }
}
