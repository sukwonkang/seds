package com.hgyu.seds.data

import java.io.Serializable

data class Dinosaur(val id: String ="",val x: Float =0f,val y: Float=0f,val baseSize: Float=0f,val color: Int=0,val width: Int=0,val height: Int=0,val sizekb : Float=0f, val img: String=""): Serializable{
    fun toDTO(): Dinosaur = Dinosaur(id,x, y, baseSize, color , width, height, sizekb, img)
    fun toDino(): Dinosaur =
        Dinosaur(id,x, y, baseSize, color , width, height, sizekb, img)
}
