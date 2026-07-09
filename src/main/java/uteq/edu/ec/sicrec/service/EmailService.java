package uteq.edu.ec.sicrec.service;

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
                "Bienvenido a SICREC"
        );

        mensaje.setText(
                "Hola " + nombre + ",\n\n" +
                        "Su cuenta ha sido creada correctamente.\n\n" +
                        "Usuario: " + usuario + "\n\n" +
                        "Contraseña temporal: " + password + "\n\n" +
                        "Por seguridad deberá cambiar la contraseña en su primer inicio de sesión.\n\n" +
                        "Sistema SICREC"
        );

        mailSender.send(mensaje);

    }

}