-- Consultas de conferência usadas nos Tutoriais 09 a 12.

-- Parte 09/10: usuários e perfis (a senha deve aparecer como hash BCrypt "$2a$...")
SELECT id, nome, email, perfil, participante_id FROM usuario ORDER BY id;
SELECT id, email, senha FROM usuario ORDER BY id;

-- Parte 11: vínculo usuário -> participante
SELECT u.email, u.perfil, p.id AS participante_id, p.nome
FROM usuario u LEFT JOIN participante p ON p.id = u.participante_id
ORDER BY u.id;

-- Parte 12: inscrições canceladas continuam na tabela
SELECT i.id, i.evento_id, i.participante_id, i.status,
       i.data_inscricao, i.data_cancelamento
FROM inscricao i
ORDER BY i.id DESC;

-- Parte 12 (item 4): só é necessário se existirem inscrições antigas sem status.
-- A aplicação já faz isso sozinha ao iniciar (CargaInicial).
UPDATE inscricao SET status = 'ATIVA' WHERE status IS NULL;
