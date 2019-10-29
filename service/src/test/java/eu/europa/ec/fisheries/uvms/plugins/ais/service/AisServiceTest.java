package eu.europa.ec.fisheries.uvms.plugins.ais.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;

@RunWith(MockitoJUnitRunner.class)
public class AisServiceTest {
    
    @Mock
    private StartupBean startUp;

    @Mock
    ProcessService processService;

    @Mock
    private ExchangeService exchangeService;

    @InjectMocks
    private AisService aisService;

    @Test
    public void sendDownSampledMovementsTest() {
        assertThat(aisService.getDownSampledMovements().size(), is(0));
        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");
        aisService.addToDownSampledMovements(movementBaseType);
        assertThat(aisService.getDownSampledMovements().size(), is(1));
        
        aisService.sendDownSampledMovements();
        assertThat(aisService.getDownSampledMovements().size(), is(0));
    }
    
    @Test
    public void sendAssetUpdatesTest() {
        when(startUp.isEnabled()).thenReturn(true);
        assertThat(aisService.getStoredAssetInfo().size(), is(0));
        aisService.getStoredAssetInfo().put("123456", new AssetDTO());
        assertThat(aisService.getStoredAssetInfo().size(), is(1));
        
        aisService.sendAssetUpdates();
        assertThat(aisService.getStoredAssetInfo().size(), is(0));
    }
}
