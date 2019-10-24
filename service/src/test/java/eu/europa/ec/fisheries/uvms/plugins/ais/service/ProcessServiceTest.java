package eu.europa.ec.fisheries.uvms.plugins.ais.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;

@RunWith(MockitoJUnitRunner.class)
public class ProcessServiceTest {

    @Mock
    private StartupBean startUp;

    @Mock
    private AisService aisService;
    
    @Mock
    private ExchangeService exchangeService;
    
    @InjectMocks
    private ProcessService processService;
    
    @Test
    public void positionTest() {
        processService.processMessages(Arrays.asList(getAisPositionMessage()));
        ArgumentCaptor<MovementBaseType> movementCaptor = ArgumentCaptor.forClass(MovementBaseType.class);
        Mockito.verify(aisService).addToDownSampledMovements(movementCaptor.capture());
        MovementBaseType movement = movementCaptor.getValue();
        assertThat(movement.getMmsi(), is("219024194"));
        assertThat(movement.getFlagState(), is("DNK"));
        assertThat(movement.getPosition().getLatitude(), is(57.490381666666664));
        assertThat(movement.getPosition().getLongitude(), is(10.685565));
    }
    
    @Test
    public void assetInformationTest() {
        String knownMmsi = "261061000";
        Map<String, AssetDTO> assetMap = new HashMap<>();
        when(aisService.getStoredAssetInfo()).thenReturn(assetMap);
        processService.processMessages(Arrays.asList(getAssetInformationAisMessage()));
        assertThat(assetMap.size(), is(1));
        AssetDTO asset = assetMap.get(knownMmsi);
        assertThat(asset.getFlagStateCode(), is("POL"));
        assertThat(asset.getVesselType(), is("Fishing"));
    }
    
    @Test
    public void fishingVesselTest() {
        String knownMmsi = "261061000";
        Set<String> fishingVessels = new HashSet<>();
        when(aisService.getKnownFishingVessels()).thenReturn(fishingVessels);
        processService.processMessages(Arrays.asList(getAssetInformationAisMessage()));
        assertThat(fishingVessels.size(), is(1));
        String assetMmsi = fishingVessels.iterator().next();
        assertThat(assetMmsi, is(knownMmsi));
    }
    
    private String getAssetInformationAisMessage() {
        return "5CpuqR029m2U<pLP00084i@T<40000000000000N1HN814lf0<1i6CR@@PC52@ii6CR@@00";
    }
    
    private String getAisPositionMessage() {
        return "13@p;@P0020hrRFPqG5EQUHHP00,0*5C";
    }
}
