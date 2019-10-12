

fun everyNth(list: List<Any>, N: Int): List<Any> {


    var temp_list : List<Any> = listOf()

    for(index in list.indices) {

        if((index + 1) % N == 0)
	   temp_list += list[index]
    }

    
    return temp_list
}


fun main() {
    var items = listOf(1,2,3,4,5,6,7,8,9)
    var result = everyNth(items,2)
    println(result)
}