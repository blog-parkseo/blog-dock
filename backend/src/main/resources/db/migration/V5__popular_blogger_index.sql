-- HOME-04 인기 블로거: 최근 7일 새 구독자 수를 구독한 시각으로 골라 센다.
CREATE INDEX idx_subscription_created_at ON subscription (created_at);
