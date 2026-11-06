package com.boutique.backend.venta.mapper;

import java.util.Locale;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.venta.dto.SerieFacturaCreateRequest;
import com.boutique.backend.venta.dto.SerieFacturaResponse;
import com.boutique.backend.venta.dto.SerieFacturaUpdateRequest;
import com.boutique.backend.venta.entity.SerieFactura;
import org.springframework.stereotype.Component;

@Component
public class SerieFacturaMapper {

    public SerieFactura toEntity(SerieFacturaCreateRequest request, Sucursal sucursal) {
        SerieFactura serie = new SerieFactura();
        serie.setSucursal(sucursal);
        serie.setPrefijo(request.prefijo().toUpperCase(Locale.ROOT));
        serie.setDigitos(request.digitos());
        serie.setSiguienteNumero(request.siguienteNumero());
        serie.setActiva(request.activa());
        return serie;
    }

    public void updateEntity(SerieFacturaUpdateRequest request, SerieFactura serie) {
        serie.setPrefijo(request.prefijo().toUpperCase(Locale.ROOT));
        serie.setDigitos(request.digitos());
        serie.setActiva(request.activa());
    }

    public SerieFacturaResponse toResponse(SerieFactura serie) {
        return new SerieFacturaResponse(serie.getId(), serie.getSucursal().getId(), serie.getPrefijo(),
            serie.getDigitos(), serie.getSiguienteNumero(), serie.getActiva());
    }
}
