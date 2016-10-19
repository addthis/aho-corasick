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

/**
 * Simple interface for mapping characters to States.
 */
interface EdgeList {
    State get(char ch);

    void put(char ch, State state);

    char[] keys();

    int size();

    Collection<State> values();

    void addChar(char c, State state);
}
