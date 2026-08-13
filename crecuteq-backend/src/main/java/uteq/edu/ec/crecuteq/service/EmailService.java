package uteq.edu.ec.crecuteq.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarPasswordTemporal(
            String destino,
            String nombre,
            String usuario,
            String password
    ) {

        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setTo(destino);

        mensaje.setSubject(
                "Bienvenido a CRECUTEQ"
        );

        mensaje.setText(
                "Hola " + nombre + ",\n\n" +
                        "Su cuenta ha sido creada correctamente.\n\n" +
                        "Usuario: " + usuario + "\n\n" +
                        "Contraseña temporal: " + password + "\n\n" +
                        "Por seguridad deberá cambiar la contraseña en su primer inicio de sesión.\n\n" +
                        "Sistema CRECUTEQ"
        );

        mailSender.send(mensaje);

    }

    public void enviarRecuperacionPassword(
            String destino,
            String nombre,
            String enlace
    ) {

        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setTo(destino);

        mensaje.setSubject(
                "Recuperación de contraseña - CRECUTEQ"
        );

        mensaje.setText(
                "Hola " + nombre + ",\n\n" +
                        "Se ha solicitado restablecer la contraseña de su cuenta.\n\n" +
                        "Para establecer una nueva contraseña, ingrese al siguiente enlace:\n\n" +
                        enlace + "\n\n" +
                        "Este enlace expirará en 30 minutos.\n\n" +
                        "Si usted no realizó esta solicitud, ignore este correo.\n\n" +
                        "Sistema CRECUTEQ"
        );

        mailSender.send(mensaje);

    }

}
