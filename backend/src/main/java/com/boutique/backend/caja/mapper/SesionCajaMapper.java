package com.boutique.backend.caja.mapper;

import java.math.RoundingMode;

import com.boutique.backend.caja.dto.SesionCajaAperturaRequest;
import com.boutique.backend.caja.dto.SesionCajaResponse;
import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class SesionCajaMapper {

    public SesionCaja toEntity(SesionCajaAperturaRequest request, Sucursal sucursal, Usuario usuario) {
        SesionCaja caja = new SesionCaja();
        caja.setSucursal(sucursal);
        caja.setAbiertoPor(usuario);
        caja.setEfectivoInicial(request.efectivoInicial().setScale(2, RoundingMode.UNNECESSARY));
        caja.setNotas(request.notas() == null || request.notas().isBlank() ? null : request.notas().trim());
        return caja;
    }

    public SesionCajaResponse toResponse(SesionCaja caja) {
        return new SesionCajaResponse(caja.getId(), caja.getSucursal().getId(), caja.getAbiertoPor().getId(),
            caja.getCerradoPor() == null ? null : caja.getCerradoPor().getId(), caja.getEstado(),
            caja.getAbiertoEn(), caja.getCerradoEn(), caja.getEfectivoInicial(), caja.getEfectivoCierre(),
            caja.getEfectivoEsperado(), caja.getDiferencia(), caja.getTotalVentas(), caja.getTotalEfectivo(),
            caja.getTotalTarjeta(), caja.getTotalTransferencia(), caja.getCantidadVentas(), caja.getNotas());
    }
}
