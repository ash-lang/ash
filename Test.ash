//import java.lang.Iterable

class Person(name : String, age : int)

class Test {

	func makePerson(name : String, age : int = 0) : Person? -> null
	var person = makePerson("Sam", 19) ?? Person("Sam", 19)
	
}