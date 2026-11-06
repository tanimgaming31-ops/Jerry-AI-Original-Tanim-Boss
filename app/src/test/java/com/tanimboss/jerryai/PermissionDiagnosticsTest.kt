// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import com.tanimboss.jerryai.diagnostics.AccessState
import com.tanimboss.jerryai.diagnostics.permissionState
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionDiagnosticsTest {
    @Test fun undeclaredPermissionIsNotPresentedAsDenied() {
        assertEquals(AccessState.NOT_DECLARED, permissionState(declared = false, granted = false))
    }
    @Test fun undeclaredPermissionCannotBePresentedAsGranted() {
        assertEquals(AccessState.NOT_DECLARED, permissionState(declared = false, granted = true))
    }
    @Test fun declaredDeniedPermissionIsReported() {
        assertEquals(AccessState.DENIED, permissionState(declared = true, granted = false))
    }
    @Test fun declaredGrantedPermissionIsReported() {
        assertEquals(AccessState.GRANTED, permissionState(declared = true, granted = true))
    }
    @Test fun versionSpecificPermissionIsNotRequiredOnOlderAndroid() {
        assertEquals(AccessState.NOT_APPLICABLE, permissionState(declared = false, granted = false, applicable = false))
    }
}
