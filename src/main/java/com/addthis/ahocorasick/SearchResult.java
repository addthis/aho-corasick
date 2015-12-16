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

import java.util.Set;

/**
 * <p>
 * Holds the result of the search so far. Includes the outputs where the search
 * finished as well as the last index of the matching.
 * </p>
 * <p>
 * <p>
 * (Internally, it also holds enough state to continue a running search, though
 * this is not exposed for public use.)
 * </p>
 */
class SearchResult {
    final State lastMatchedState;
    final String chars;

    /**
     * The index where the search last terminated.
     */
    final int lastIndex;

    SearchResult(State s, String cs, int i) {
        this.lastMatchedState = s;
        this.chars = cs;
        this.lastIndex = i;
    }

    /**
     * Returns a list of the outputs of this match.
     */
    Set<Object> getOutputs() {
        return lastMatchedState.getOutputs();
    }
}
