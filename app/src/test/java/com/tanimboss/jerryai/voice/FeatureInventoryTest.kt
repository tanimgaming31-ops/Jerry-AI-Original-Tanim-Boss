// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import com.tanimboss.jerryai.catalog.*
import org.junit.*
class FeatureInventoryTest {
    @Test fun all54ApprovedGroupsArePresentWithUniqueStableMasterIds() {
        Assert.assertEquals((1..54).toList(),FeatureCatalog.features.map{it.id})
        Assert.assertTrue(FeatureCatalog.features.all{it.phase in 1..7})
        Assert.assertEquals(listOf(1,2,3,8,9,10),FeatureCatalog.features.filter{it.phase==1}.map{it.id})
    }
    @Test fun deviceVerifiedCannotBeClaimedWithoutEvidence() {
        Assert.assertThrows(IllegalArgumentException::class.java){Feature(1,"test",1,ImplementationState.WORKING_DEVICE_VERIFIED)}
        Assert.assertTrue(FeatureCatalog.features.none{it.implementation==ImplementationState.WORKING_DEVICE_VERIFIED})
    }
    @Test fun phase1SourceStatusDoesNotClaimOtherVoicePhases() {
        Assert.assertTrue(FeatureCatalog.features.filter{it.id in listOf(4,5,6,7)}.all{it.implementation==ImplementationState.PLANNED})
        Assert.assertTrue(FeatureCatalog.features.filter{it.id in listOf(1,2,3,8,10)}.all{it.implementation==ImplementationState.IMPLEMENTED_DEVICE_UNVERIFIED})
    }
}
