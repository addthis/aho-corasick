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

import java.util.Collection;

import com.gs.collections.impl.map.mutable.primitive.CharObjectHashMap;

class SparseEdgeList implements EdgeList {

    private final CharObjectHashMap<State> states;

    public SparseEdgeList() {
        states = new CharObjectHashMap<>(1);
    }

    @Override
    public State get(char c) {
        return states.get(c);
    }

    @Override
    public void put(char c, State s) {
        states.put(c, s);
    }

    @Override
    public int size() { return states.size(); }

    @Override
    public char[] keys() {
        return states.keySet().toArray();
    }

    @Override
    public Collection<State> values() { return states.values(); }

}
