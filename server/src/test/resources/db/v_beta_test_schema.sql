CREATE DATABASE IF NOT EXISTS V_Beta_Test;
USE V_Beta_Test;

CREATE TABLE Gym_Role (
	role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_type VARCHAR(25)
);

CREATE TABLE Gym_Action(
	action_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_definition varchar(50)
);

CREATE TABLE Role_Permission(
	role_id BIGINT,
    action_id BIGINT,
    FOREIGN KEY (role_id) REFERENCES Gym_Role (role_id),
    FOREIGN KEY (action_id) REFERENCES Gym_Action (action_id)
);

CREATE TABLE User_Account(
	user_id BIGINT PRIMARY KEY  AUTO_INCREMENT,
    username VARCHAR(25) NOT NULL,
    email VARCHAR(225) NOT NULL,
    firebase_uid VARCHAR(128) NOT NULL UNIQUE,
    gym_role_id BIGINT,
    FOREIGN KEY (gym_role_id) REFERENCES Gym_Role (role_id)
);


CREATE TABLE Climbing_Grade(
	grade_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    grade VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE Wall_Section(
	wall_section_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    info VARCHAR(250),
    wall_section_name VARCHAR(30) NOT NULL
);

CREATE TABLE Climbing_Problem(
	problem_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hold_color VARCHAR(25) NOT NULL,
    info VARCHAR(250),
    lifecycle_status VARCHAR(25),
    create_date DATETIME,
    wall_section_id BIGINT,
    assigned_grade_id BIGINT,
    FOREIGN KEY (wall_section_id) REFERENCES Wall_Section (wall_section_id),
    FOREIGN KEY (assigned_grade_id) REFERENCES Climbing_Grade (grade_id)
);

CREATE TABLE User_Perceive_Grade(
	user_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, problem_id),
    FOREIGN KEY (user_id) REFERENCES User_Account (user_id),
    FOREIGN KEY (problem_id) REFERENCES Climbing_Problem (problem_id),
    FOREIGN KEY (grade_id) REFERENCES Climbing_Grade (grade_id)
);

CREATE TABLE User_Beta(
	user_beta_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    problem_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES User_Account (user_id),
    FOREIGN KEY (problem_id) REFERENCES Climbing_Problem (problem_id)
);

CREATE TABLE Solution_Beta(
	beta_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_beta_id BIGINT UNIQUE NOT NULL,
    beta_name VARCHAR(125) NOT NULL,
    video_url VARCHAR(150) NOT NULL UNIQUE,
    create_date DATETIME,
    FOREIGN KEY (user_beta_id) REFERENCES User_Beta (user_beta_id)
);

CREATE TABLE User_Comment(
	user_comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    problem_id BIGINT,
	FOREIGN KEY (user_id) REFERENCES User_Account (user_id),
    FOREIGN KEY (problem_id) REFERENCES Climbing_Problem (problem_id)
);

CREATE TABLE Discussion_Comment(
	discussion_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_comment_id BIGINT UNIQUE NOT NULL,
    info VARCHAR(250),
    create_date DATETIME,
    FOREIGN KEY (user_comment_id) REFERENCES User_Comment (user_comment_id)
);


INSERT INTO Gym_Role(role_type)
	VALUES ('CLIMBER'), ('SETTER'), ('ADMIN');
    
INSERT INTO Gym_Action (action_definition)
	VALUES ('CREATE_BETA'), ('DELETE_BETA'), ('CREATE_COMMENT'), ('DELETE_COMMENT'),
    ('CREATE_PROBLEM'), ('DELETE_PROBLEM'), ('RESET_WALL'), ('CREATE_WALL'), ('DELETE_WALL'),
    ('CHANGE_ROLE'), ('GRADE_PROBLEM');

INSERT INTO Climbing_Grade(grade) 
	VALUES ('VB'), ('V0'), ('V1'), ('V2'), ('V3'), ('V4'),
	('V5'), ('V6'), ('V7'), ('V8'), ('V9'), ('V10'), ('V11'),
    ('V12'), ('V13'), ('V14'), ('V15'), ('V16'), ('V17');

INSERT INTO Role_Permission (role_id, action_id) 
	VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 11),
    (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6),
    (2, 7), (2, 11), (3, 1), (3, 2), (3, 3), (3, 4),
    (3, 8), (3, 9), (3, 10), (3, 11);
    
INSERT INTO Wall_Section (info, wall_section_name) 
	VALUES ('This is a test wall section', 'Test Wall');
    
SELECT * FROM Climbing_Grade ORDER BY grade_id;

INSERT INTO Climbing_Problem(hold_color, info, lifecycle_status, create_date, wall_section_id, assigned_grade_id)
	VALUES ('BLACK', 'BLACK VB-V0', 'ACTIVE', NOW(), 1, 1),
    ('RED', 'RED V0-V1', 'ACTIVE', NOW(), 1, 2);
    
INSERT INTO Climbing_Problem(hold_color, info, lifecycle_status, create_date, wall_section_id, assigned_grade_id)
	VALUES ('BLACK', 'BLACK V8-V9', 'ARCHIVE', NOW(), 1, 10);

INSERT INTO User_Account(username, email, firebase_uid, gym_role_id) 
	VALUES ('testUser', 'testUser@gmail.com', 'testFirebaseUid', 1);
    
INSERT INTO User_Account(username, email, firebase_uid, gym_role_id) 
	VALUES ('testSetter', 'testSetter@gmail.com', 'testFirebaseUid2', 2),
		('testAdmin', 'testAdmin@gmail.com', 'testFirebaseUid3', 3);

INSERT INTO User_Perceive_Grade(user_id, grade_id, problem_id)
	VALUES (1, 3, 1),
		(2, 3, 1),
        (3, 6, 1);
        
INSERT INTO User_Comment(user_id, problem_id)
	VALUES (1, 1),
		(2, 1),
        (3, 1);
        
INSERT INTO Discussion_Comment(user_comment_id, info, create_date)
	VALUES (1, 'Fun problem!!!', NOW()),
		(2, 'Good Warm-up', NOW()),
        (3, 'Maybe a little too hard :(', NOW());
        
INSERT INTO User_Beta(user_id, problem_id)
	VALUES(1, 1);

INSERT INTO Solution_Beta(user_beta_id, beta_name, video_url, create_date)
	VALUES(1, 'IMG_772.mp4', 'https://storage.googleapis.com/beta_videos_public/problem-1/IMG_772.mp4', NOW());
    
SELECT * FROM Solution_Beta;