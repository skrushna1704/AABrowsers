import java.util.*

data class Student(
    var id: Int,
    var name: String,
    var age: Int,
    var marks: Double,
    var grade: String
)

class StudentManagement {

    private val students = mutableListOf<Student>()

    fun addStudent(scanner: Scanner) {
        print("Enter ID: ")
        val id = scanner.nextInt()
        scanner.nextLine()

        if (students.any { it.id == id }) {
            println("Student ID already exists!")
            return
        }

        print("Enter Name: ")
        val name = scanner.nextLine()

        print("Enter Age: ")
        val age = scanner.nextInt()

        print("Enter Marks: ")
        val marks = scanner.nextDouble()

        val grade = calculateGrade(marks)

        students.add(Student(id, name, age, marks, grade))

        println("\nStudent Added Successfully.\n")
    }

    fun displayStudents() {
        if (students.isEmpty()) {
            println("No Students Found.\n")
            return
        }

        println("\n---------------- Student List ----------------")

        for (student in students) {
            println("ID     : ${student.id}")
            println("Name   : ${student.name}")
            println("Age    : ${student.age}")
            println("Marks  : ${student.marks}")
            println("Grade  : ${student.grade}")
            println("---------------------------------------------")
        }
    }

    fun searchStudent(scanner: Scanner) {
        print("Enter Student ID: ")
        val id = scanner.nextInt()

        val student = students.find { it.id == id }

        if (student != null) {
            println("\nStudent Found")
            println(student)
        } else {
            println("Student Not Found.")
        }
    }

    fun updateStudent(scanner: Scanner) {
        print("Enter Student ID: ")
        val id = scanner.nextInt()

        val student = students.find { it.id == id }

        if (student == null) {
            println("Student Not Found.")
            return
        }

        scanner.nextLine()

        print("Enter New Name: ")
        student.name = scanner.nextLine()

        print("Enter New Age: ")
        student.age = scanner.nextInt()

        print("Enter New Marks: ")
        student.marks = scanner.nextDouble()

        student.grade = calculateGrade(student.marks)

        println("Student Updated Successfully.")
    }

    fun deleteStudent(scanner: Scanner) {
        print("Enter Student ID: ")
        val id = scanner.nextInt()

        val removed = students.removeIf { it.id == id }

        if (removed)
            println("Student Deleted Successfully.")
        else
            println("Student Not Found.")
    }

    fun sortByMarks() {

        students.sortByDescending { it.marks }

        println("\nStudents Sorted By Marks (High to Low)\n")

        displayStudents()
    }

    fun showTopper() {

        if (students.isEmpty()) {
            println("No Students Available.")
            return
        }

        val topper = students.maxByOrNull { it.marks }

        println("\nTopper Details")
        println(topper)
    }

    fun calculateAverage() {

        if (students.isEmpty()) {
            println("No Students Available.")
            return
        }

        val average = students.map { it.marks }.average()

        println("Average Marks = %.2f".format(average))
    }

    private fun calculateGrade(marks: Double): String {

        return when {
            marks >= 90 -> "A+"
            marks >= 80 -> "A"
            marks >= 70 -> "B"
            marks >= 60 -> "C"
            marks >= 50 -> "D"
            else -> "Fail"
        }
    }
}

fun main() {

    val scanner = Scanner(System.`in`)
    val management = StudentManagement()

    while (true) {

        println("\n========== Student Management System ==========")
        println("1. Add Student")
        println("2. Display Students")
        println("3. Search Student")
        println("4. Update Student")
        println("5. Delete Student")
        println("6. Sort By Marks")
        println("7. Show Topper")
        println("8. Average Marks")
        println("9. Exit")

        print("Enter Choice: ")

        when (scanner.nextInt()) {

            1 -> management.addStudent(scanner)

            2 -> management.displayStudents()

            3 -> management.searchStudent(scanner)

            4 -> management.updateStudent(scanner)

            5 -> management.deleteStudent(scanner)

            6 -> management.sortByMarks()

            7 -> management.showTopper()

            8 -> management.calculateAverage()

            9 -> {
                println("Thank You!")
                break
            }

            else -> println("Invalid Choice.")
        }
    }
}
