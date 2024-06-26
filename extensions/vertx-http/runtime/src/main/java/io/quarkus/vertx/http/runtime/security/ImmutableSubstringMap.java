package io.quarkus.vertx.http.runtime.security;

import java.util.Arrays;

import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher.PathMatch;

/**
 * A string keyed map that can be accessed as a substring, eliminating the need to allocate a new string
 * to do a key comparison against.
 */
public class ImmutableSubstringMap<V> {

    private static final int ALL_BUT_LAST_BIT = ~1;
    private final Object[] table;

    ImmutableSubstringMap(Object[] table) {
        this.table = Arrays.copyOf(table, table.length);
    }

    @SuppressWarnings("unchecked")
    public SubstringMatch<V> get(String key, int length) {
        if (key.length() < length) {
            throw new IllegalArgumentException();
        }
        int hash = hash(key, length);
        int pos = tablePos(table, hash);
        int start = pos;
        while (table[pos] != null) {
            if (doEquals((String) table[pos], key, length)) {
                SubstringMatch<V> match = (SubstringMatch<V>) table[pos + 1];
                if (match == null) {
                    return null;
                }
                if (match.hasSubPathMatcher) {
                    // consider request path '/one/two/three/four/five'
                    // 'match.key' (which is prefix path) never ends with a slash, e.g. 'match.key=/one/two'
                    // which means index 'match.key.length()' is index of the last char of the '/one/two/' sub-path
                    // considering we are looking for a path segment after '/one/two/*', that is the first char
                    // of the '/four/five' sub-path, the separator index must be greater than 'match.key.length() + 1'
                    if (key.length() > (match.key.length() + 1)) {
                        // let say match key is '/one/two'
                        // then next path segment is '/four' and '/three' is skipped
                        // for path pattern was like: '/one/two/*/four/five'
                        int nextPathSegmentIdx = key.indexOf('/', match.key.length() + 1);
                        if (nextPathSegmentIdx != -1) {
                            // following the example above, 'nextPath' would be '/four/five'
                            // and * matched 'three' path segment characters
                            String nextPath = key.substring(nextPathSegmentIdx);
                            PathMatch<SubstringMatch<V>> subMatch = match.subPathMatcher.match(nextPath);
                            if (subMatch.getValue() != null) {
                                return subMatch.getValue();
                            }
                        }
                    }

                    if (match.value == null) {
                        // paths with inner wildcard didn't match
                        // and there is no prefix path with ending wildcard either
                        return null;
                    }
                }
                // prefix path with ending wildcard: /one/two*
                return match;
            }
            pos += 2;
            if (pos >= table.length) {
                pos = 0;
            }
            if (pos == start) {
                return null;
            }
        }
        return null;
    }

    static int tablePos(Object[] table, int hash) {
        return (hash & (table.length - 1)) & ALL_BUT_LAST_BIT;
    }

    static boolean doEquals(String s1, String s2, int length) {
        if (s1.length() != length || s2.length() < length) {
            return false;
        }
        for (int i = 0; i < length; ++i) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    static int hash(String value, int length) {
        if (length == 0) {
            return 0;
        }
        int h = 0;
        for (int i = 0; i < length; i++) {
            h = 31 * h + value.charAt(i);
        }
        return h;
    }

    public static final class SubstringMatch<V> {
        private final String key;
        private final V value;
        private final boolean hasSubPathMatcher;
        private final ImmutablePathMatcher<SubstringMatch<V>> subPathMatcher;

        SubstringMatch(String key, V value) {
            this.key = key;
            this.value = value;
            this.subPathMatcher = null;
            this.hasSubPathMatcher = false;
        }

        SubstringMatch(String key, V value, ImmutablePathMatcher<SubstringMatch<V>> subPathMatcher) {
            this.key = key;
            this.value = value;
            this.subPathMatcher = subPathMatcher;
            this.hasSubPathMatcher = subPathMatcher != null;
        }

        public String getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        boolean hasSubPathMatcher() {
            return hasSubPathMatcher;
        }
    }
}
