package com.example.todo.health;

/**
 * Assert that a condition is satisfied
 * @param message error message
 * @param condition expected to be true
 */
fun assertTrue( message: String?, condition: Boolean) {
	if (!condition) throw IllegalStateException( message);
}

/**
 * Assert that two objects are equal
 * @param expected Expected value
 * @param actual Actual value
 */
fun assertEquals( expected: Any?, actual: Any?) {
	if (expected == null && actual == null) return;
	assertTrue(
		"Objects are different. Expected: <$expected>, Actual: <$actual>",
		expected != null && expected.equals( actual));
}
