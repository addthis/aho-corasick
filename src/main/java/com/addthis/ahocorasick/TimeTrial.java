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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

/**
 * Quick and dirty code: measures the amount of time it takes to construct an
 * AhoCorasick tree out of all the words in <tt>/usr/share/dict/words</tt>.
 */
public class TimeTrial {

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        AhoCorasick tree = AhoCorasick.builder().build();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("/usr/share/dict/words")));
        String line;
        while ((line = reader.readLine()) != null) {
            tree.add(line, line);
        }
        tree.prepare();
        long endTime = System.currentTimeMillis();
        System.out.println("endTime - startTime = " + (endTime - startTime) + " milliseconds");
    }
}
