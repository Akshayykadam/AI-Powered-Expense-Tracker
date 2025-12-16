package com.expense.tracker.data.local.sms

import org.junit.Assert.*
import org.junit.Test
import com.expense.tracker.domain.model.TransactionType

class SmsParserTest {

    private val parser = SmsParser()

    private fun createSms(body: String): RawSms {
        return RawSms(
            id = 1L,
            address = "VM-TEST",
            body = body,
            date = 0L
        )
    }

    @Test
    fun `test Rs pattern`() {
        val sms = createSms("Rs. 500.00 debited")
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        assertEquals(500.0, (result as ParseResult.Success).amount, 0.01)
    }

    @Test
    fun `test INR pattern`() {
        val sms = createSms("INR 1250 spent")
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        assertEquals(1250.0, (result as ParseResult.Success).amount, 0.01)
    }

    @Test
    fun `test Amt pattern`() {
        val sms = createSms("Amt: 100.50")
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        assertEquals(100.50, (result as ParseResult.Success).amount, 0.01)
    }
    
    @Test
    fun `test loose number with decimals`() {
        val sms = createSms("Account debited by 399.00 for recharge")
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        assertEquals(399.0, (result as ParseResult.Success).amount, 0.01)
    }
    
    @Test
    fun `test failure on non-amount`() { // Verify it doesn't match garbage
        val sms = createSms("Hello world this is a text without numbers")
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Failure)
    }
}
