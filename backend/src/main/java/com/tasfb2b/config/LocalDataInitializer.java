package com.tasfb2b.config;

import com.tasfb2b.service.CargaDatosDbService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
public class LocalDataInitializer implements ApplicationRunner {

    private final CargaDatosDbService cargaDatosDbService;

    public LocalDataInitializer(CargaDatosDbService cargaDatosDbService) {
        this.cargaDatosDbService = cargaDatosDbService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("[LocalDataInitializer] Verificando datos iniciales en la base local...");
        cargaDatosDbService.cargarAeropuertos();
        cargaDatosDbService.cargarVuelos();
        cargaDatosDbService.cargarEnvios();
        System.out.println("[LocalDataInitializer] Datos listos.");
    }
}
