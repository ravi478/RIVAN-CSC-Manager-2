package com.rivan.cscmanager

import com.rivan.cscmanager.data.Roles
import org.junit.Assert.assertEquals
import org.junit.Test

class RolesTest {
    @Test fun appHasExactlyTheTwoAgreedRoleIdentifiers() {
        assertEquals("ADMIN", Roles.ADMIN)
        assertEquals("OPERATOR", Roles.OPERATOR)
    }
}
