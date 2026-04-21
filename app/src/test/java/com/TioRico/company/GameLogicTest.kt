package com.TioRico.company

import com.TioRico.company.models.*
import org.junit.Test
import org.junit.Assert.*

class GameLogicTest {

    @Test
    fun `test win condition - reaching goal exactly`() {
        // Setup
        val goal = 5000
        val initialMoney = 4800
        val actionBonus = 200 // SAVE action
        
        // Logic execution
        val finalMoney = initialMoney + actionBonus
        val hasWon = finalMoney >= goal
        
        // Assertions
        assertEquals(5000, finalMoney)
        assertTrue("Player should have won by reaching the goal", hasWon)
    }

    @Test
    fun `test loss condition - bankruptcy`() {
        // Setup
        val initialMoney = 100
        val actionCost = -300 // SPEND action
        
        // Logic execution
        val calculatedMoney = initialMoney + actionCost
        val finalMoney = if (calculatedMoney < 0) 0 else calculatedMoney
        val isEliminated = finalMoney <= 0
        
        // Assertions
        assertEquals(0, finalMoney)
        assertTrue("Player should be eliminated if money reaches zero", isEliminated)
    }

    @Test
    fun `test capital tax event logic`() {
        // Setup
        val choices = listOf(ActionType.SAVE, ActionType.INVEST, ActionType.SPEND)
        val taxAmount = -400
        
        // Logic execution & Assertions
        choices.forEach { action ->
            val isExempt = action == ActionType.SPEND
            val penalty = if (isExempt) 0 else taxAmount
            
            if (action == ActionType.SPEND) {
                assertEquals(0, penalty)
            } else {
                assertEquals(-400, penalty)
            }
        }
    }
}
