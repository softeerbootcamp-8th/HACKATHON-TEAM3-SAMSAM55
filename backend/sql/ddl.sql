CREATE TABLE `trip` (
                        `id`	BIGINT	NOT NULL,
                        `host_user_id`	BIGINT	NOT NULL,
                        `title`	VARCHAR(100)	NOT NULL,
                        `start_date`	DATETIME	NOT NULL,
                        `end_date`	DATETIME	NOT NULL,
                        `companion_count`	INT	NOT NULL,
                        `invite_code`	VARCHAR(64)	NOT NULL,
                        `created_at`	DATETIME	NOT NULL,
                        `updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `vote` (
                        `id`	BIGINT	NOT NULL,
                        `option_id`	BIGINT	NOT NULL,
                        `created_at`	DATETIME	NOT NULL,
                        `updated_at`	DATETIME	NOT NULL,
                        `itinerary_item_id`	BIGINT	NOT NULL,
                        `participant_id`	BIGINT	NOT NULL
);

CREATE TABLE `participant` (
                               `id`	BIGINT	NOT NULL,
                               `trip_id`	BIGINT	NOT NULL,
                               `role_name`	VARCHAR(50)	NOT NULL,
                               `joined_at`	DATETIME	NULL
);

CREATE TABLE `vote_option` (
                               `id`	BIGINT	NOT NULL,
                               `itinerary_item_id`	BIGINT	NOT NULL,
                               `name`	VARCHAR(100)	NOT NULL,
                               `description`	TEXT	NULL,
                               `description_source`	VARCHAR(20)	NOT NULL,
                               `image`	LONGBLOB	NULL,
                               `image_content_type`	VARCHAR(100)	NULL,
                               `tags`	TEXT	NULL,
                               `created_at`	DATETIME	NOT NULL,
                               `updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `trip_day` (
                            `id`	BIGINT	NOT NULL,
                            `trip_id`	BIGINT	NOT NULL,
                            `day_number`	INT	NOT NULL,
                            `trip_date`	DATE	NOT NULL
);

CREATE TABLE `itinerary_item` (
                                  `id`	BIGINT	NOT NULL,
                                  `trip_day_id`	BIGINT	NOT NULL,
                                  `name`	VARCHAR(100)	NOT NULL,
                                  `category`	VARCHAR(50)	NULL,
                                  `decision_type`	VARCHAR(20)	NOT NULL,
                                  `status`	VARCHAR(20)	NOT NULL,
                                  `sort_order`	INT	NOT NULL,
                                  `confirmed_option_id`	BIGINT	NULL,
                                  `created_at`	DATETIME	NOT NULL,
                                  `updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `users` (
                         `id`	BIGINT	NOT NULL,
                         `login_id`	VARCHAR(100)	NOT NULL,
                         `password_hash`	VARCHAR(255)	NOT NULL,
                         `created_at`	DATETIME	NOT NULL,
                         `updated_at`	DATETIME	NOT NULL
);

ALTER TABLE `trip` ADD CONSTRAINT `PK_TRIP` PRIMARY KEY (
                                                         `id`
    );

ALTER TABLE `vote` ADD CONSTRAINT `PK_VOTE` PRIMARY KEY (
                                                         `id`
    );

ALTER TABLE `participant` ADD CONSTRAINT `PK_PARTICIPANT` PRIMARY KEY (
                                                                       `id`
    );

ALTER TABLE `vote_option` ADD CONSTRAINT `PK_VOTE_OPTION` PRIMARY KEY (
                                                                       `id`
    );

ALTER TABLE `trip_day` ADD CONSTRAINT `PK_TRIP_DAY` PRIMARY KEY (
                                                                 `id`
    );

ALTER TABLE `itinerary_item` ADD CONSTRAINT `PK_ITINERARY_ITEM` PRIMARY KEY (
                                                                             `id`
    );

ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
                                                           `id`
    );

ALTER TABLE `trip` ADD CONSTRAINT `FK_users_TO_trip_1` FOREIGN KEY (
                                                                    `host_user_id`
    )
    REFERENCES `users` (
                        `id`
        );

ALTER TABLE `vote` ADD CONSTRAINT `FK_vote_option_TO_vote_1` FOREIGN KEY (
                                                                          `option_id`
    )
    REFERENCES `vote_option` (
                              `id`
        );

ALTER TABLE `vote` ADD CONSTRAINT `FK_itinerary_item_TO_vote_1` FOREIGN KEY (
                                                                             `itinerary_item_id`
    )
    REFERENCES `itinerary_item` (
                                 `id`
        );

ALTER TABLE `vote` ADD CONSTRAINT `FK_participant_TO_vote_1` FOREIGN KEY (
                                                                          `participant_id`
    )
    REFERENCES `participant` (
                              `id`
        );

ALTER TABLE `participant` ADD CONSTRAINT `FK_trip_TO_participant_1` FOREIGN KEY (
                                                                                 `trip_id`
    )
    REFERENCES `trip` (
                       `id`
        );

ALTER TABLE `vote_option` ADD CONSTRAINT `FK_itinerary_item_TO_vote_option_1` FOREIGN KEY (
                                                                                           `itinerary_item_id`
    )
    REFERENCES `itinerary_item` (
                                 `id`
        );

ALTER TABLE `trip_day` ADD CONSTRAINT `FK_trip_TO_trip_day_1` FOREIGN KEY (
                                                                           `trip_id`
    )
    REFERENCES `trip` (
                       `id`
        );

ALTER TABLE `itinerary_item` ADD CONSTRAINT `FK_trip_day_TO_itinerary_item_1` FOREIGN KEY (
                                                                                           `trip_day_id`
    )
    REFERENCES `trip_day` (
                           `id`
        );

ALTER TABLE `itinerary_item` ADD CONSTRAINT `FK_vote_option_TO_itinerary_item_1` FOREIGN KEY (
                                                                                              `confirmed_option_id`
    )
    REFERENCES `vote_option` (
                              `id`
        );

