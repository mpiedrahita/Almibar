package com.reposteria.productos.model.event;

import com.reposteria.productos.model.entity.AuditoriaProducto;
import com.reposteria.productos.model.entity.ResultadoOperacion;
import com.reposteria.productos.model.repository.AuditoriaProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observador que guarda el registro de actividades (RNF-18).
 *
 * - Las operaciones exitosas se guardan solo cuando la transacción del producto se confirma.
 * - Las rechazadas se guardan en una transacción propia, para que no se pierdan con el rollback.
 */
@Component
public class AuditoriaProductoListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaProductoListener.class);

    private final AuditoriaProductoRepository repositorio;

    public AuditoriaProductoListener(AuditoriaProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alConfirmarse(ProductoEvent evento) {
        repositorio.save(new AuditoriaProducto(evento.fecha(), evento.usuario(), evento.operacion(),
                evento.productoId(), evento.nombreProducto(), ResultadoOperacion.EXITOSA, evento.detalle()));
        log.info("Auditoría: {} hizo {} sobre el producto {} ({})",
                evento.usuario(), evento.operacion(), evento.productoId(), evento.detalle());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRechazarse(OperacionRechazadaEvent evento) {
        repositorio.save(new AuditoriaProducto(evento.fecha(), evento.usuario(), evento.operacion(),
                evento.productoId(), evento.nombreProducto(), ResultadoOperacion.RECHAZADA, evento.motivo()));
        log.info("Auditoría: se rechazó {} de {} sobre el producto {}: {}",
                evento.operacion(), evento.usuario(), evento.productoId(), evento.motivo());
    }
}
