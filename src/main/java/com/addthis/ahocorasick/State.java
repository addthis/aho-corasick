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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A state represents an element in the Aho-Corasick tree.
 */
class State {

    private static final State EMPTY_STATE = new State(-1);

    private static final char[] EMPTY_KEYS = new char[0];

    private char c;
    private final int depth;
    private EdgeList edgeList;
    private State fail;
    private Set<Object> outputs;
    private char[] fastPath;
    private State[] fastTransitions;
    private boolean incomingFail;

    State(int depth) {
        this.depth = depth;
        this.edgeList = null;
        this.fail = null;
        this.outputs = ImmutableSet.of();
    }

    State extend(char c) {
        if (edgeList == null) {
            edgeList = new SparseEdgeList();
        }
        State nextState = edgeList.get(c);
        if (nextState == null) {
            nextState = new State(depth + 1);
            edgeList.put(c, nextState);
            edgeList.addChar(c, nextState);
        }
        return nextState;
    }

    void addChar(char c) {
        this.c = c;
    }

    // DFS
    @VisibleForTesting
    public void traverse( State root ) {
        if(root == null ) return;
        System.out.print(root.c + " ");

        EdgeList edgeList = root.edgeList;
        if(edgeList == null ) return;
        for( State state : edgeList.values()) {
            traverse(state);
        }
    }

    State extendAll(String chars) {
        State state = this;
        for (int i = 0; i < chars.length(); i++) {
            if (state.edgeList == null) {
                state.edgeList = new SparseEdgeList();
            }

            State nextState = state.edgeList.get(chars.charAt(i));

            if (nextState == null) {
                nextState = state.extend(chars.charAt(i));
            }
            state = nextState;
        }
        return state;
    }

    private boolean isCompressible() {
        return (edgeList != null) && (edgeList.size() == 1) && (outputs.size() == 0) && (!incomingFail) && !isRoot();
    }

    void compressPaths() {
        if (isCompressible()) {
            StringBuilder builder = new StringBuilder();
            List<State> transitions = new ArrayList<>();
            State next = this;
            do {
                transitions.add(next.getFail());
                char nextChar = next.edgeList.keys()[0];
                next = next.edgeList.get(nextChar);
                builder.append(nextChar);
            } while (next.isCompressible());
            if (builder.length() > 1) {
                transitions.add(next);
                fastPath = builder.toString().toCharArray();
                fastTransitions = transitions.toArray(new State[transitions.size()]);
                edgeList = null;
                fail = null;
            }
            next.compressPaths();
        } else if (edgeList != null) {
            for (State next : edgeList.values()) {
                next.compressPaths();
            }
        }
    }

    /**
     * Returns the size of the tree rooted at this State. Note: do not call this
     * if there are loops in the edgelist graph, such as those introduced by
     * AhoCorasick.prepare().
     */
    int size() {
        int result = 1;
        if (edgeList == null) {
            return result;
        }
        char[] keys = edgeList.keys();
        for (char key : keys) {
            result += edgeList.get(key).size();
        }
        return result;
    }

    State get(char c) {
        assert (fastPath == null);

        State s = (edgeList != null) ? edgeList.get(c) : null;

        // The root state always returns itself when a state for the input character doesn't exist
        if ((s == null) && isRoot()) {
            return this;
        } else {
            return s;
        }
    }

    State next(String input, MutableInt currentIndex) {
        if (fastPath == null) {
            char nextChar = input.charAt(currentIndex.getAndIncrement());
            return followFailureTransitions(nextChar, this);
        } else {
            int index;
            char nextChar = 0;
            for (index = 0; index < fastPath.length; index++) {
                if (currentIndex.getValue() == input.length()) {
                    return EMPTY_STATE;
                } else {
                    nextChar = input.charAt(currentIndex.getAndIncrement());
                    if (nextChar != fastPath[index]) {
                        break;
                    }
                }
            }
            if (index == fastPath.length) {
                return fastTransitions[index];
            } else {
                return followFailureTransitions(nextChar, fastTransitions[index]);
            }
        }
    }

    private State followFailureTransitions(char nextChar, State current) {
        State next = current.get(nextChar);
        while (next == null) {
            current = current.getFail();
            next = current.get(nextChar);
        }
        return next;
    }

    char[] keys() {
        return (edgeList != null) ? edgeList.keys() : EMPTY_KEYS;
    }

    State getFail() {
        return fail;
    }

    void setFail(State f) {
        f.incomingFail = true;
        fail = f;
    }

    void addOutput(Object output) {
        if (outputs.contains(output)) {
            // do nothing
        } else if (outputs.size() == 0) {
            outputs = ImmutableSet.of(output);
        } else {
            if (outputs.size() == 1) {
                outputs = new HashSet<>(outputs);
            }
            outputs.add(output);
        }
    }

    void addOutputs(Collection<Object> newOutputs) {
        if (outputs.size() < 2) {
            outputs = new HashSet<>(outputs);
        }
        outputs.addAll(newOutputs);
    }

    Set<Object> getOutputs() {
        return outputs;
    }

    boolean isRoot() {
        return depth == 0;
    }

    int getDepth() {
        return depth;
    }

    String getFastPath() {
        return new String(fastPath);
    }

    State[] getFastTransitions() {
        return fastTransitions;
    }

}
