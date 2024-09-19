/* Copyright (C) 2024 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.core;

/**
 * Excepci&oacute;n que indica una operaci&oacute;n de autorizaci&oacute;n
 * cancelada voluntariamente por el usuario.
 */
public final class AOCancelledAuthOperationException extends AOCancelledOperationException {

	/** Serial Id. */
	private static final long serialVersionUID = 8066874774354045779L;

	/** Crea una excepci&oacute;n sin informaci&oacute;n adicional. */
    public AOCancelledAuthOperationException() {
        super();
    }

    /**
     * Crea la excepci&oacute;n con un mensaje determinado.
     * @param msg
     *        Mensaje descriptivo de la excepci&oacute;n.
     */
    public AOCancelledAuthOperationException(final String msg) {
        super(msg);
    }

    /**
     * Crea la excepci&oacute;n con un mensaje determinado y la causa que la origin&oacute;.
     * @param msg
     *        Mensaje descriptivo de la excepci&oacute;n.
     * @param e
     * 		  Causa de la excepci&oacute;n.
     */
    public AOCancelledAuthOperationException(final String msg, final Exception e) {
        super(msg, e);
    }
}
