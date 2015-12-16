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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestState {

    @Test
    public void simpleExtension() {
        State s = new State(0);
        State s2 = s.extend('a');
        assertTrue(s2 != s && s2 != null);
        assertEquals(2, s.size());
    }

    @Test
    public void simpleExtensionSparse() {
        State s = new State(50);
        State s2 = s.extend('a');
        assertTrue(s2 != s && s2 != null);
        assertEquals(2, s.size());
    }

    @Test
    public void singleState() {
        State s = new State(0);
        assertEquals(1, s.size());
    }

    @Test
    public void singleStateSparse() {
        State s = new State(50);
        assertEquals(1, s.size());
    }

    @Test
    public void extendAll() {
        State s = new State(0);
        State s2 = s.extendAll("hello world");
        assertEquals(12, s.size());
    }

    @Test
    public void extendAllTwiceDoesntAddMoreStates() {
        State s = new State(0);
        State s2 = s.extendAll("hello world");
        State s3 = s.extendAll("hello world");
        assertEquals(12, s.size());
        assertTrue(s2 == s3);
    }

    @Test
    public void extendAllTwiceDoesntAddMoreStatesSparse() {
        State s = new State(50);
        State s2 = s.extendAll("hello world");
        State s3 = s.extendAll("hello world");
        assertEquals(12, s.size());
        assertTrue(s2 == s3);
    }

    @Test
    public void addingALotOfStatesIsOk() {
        State s = new State(0);
        for (int i = 0; i < 256; i++) {
            s.extend((char) i);
        }
        assertEquals(257, s.size());
    }

    @Test
    public void addingALotOfStatesIsOkOnSparseRep() {
        State s = new State(50);
        for (int i = 0; i < 256; i++) {
            s.extend((char) i);
        }
        assertEquals(257, s.size());
    }

}
