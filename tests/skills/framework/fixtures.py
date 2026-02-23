"""
Test fixtures for skill testing.
Provides sample data for each category of skills.
"""

import json
import random
import string
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional
from dataclasses import dataclass

from .es_client import ElasticsearchTestClient


@dataclass 
class Fixture:
    """A test fixture definition."""
    name: str
    index_name: str
    mappings: Dict
    documents: List[Dict]
    description: str = ""


class FixtureManager:
    """Manages test fixtures for skill testing."""
    
    def __init__(self, es_client: ElasticsearchTestClient):
        self.es_client = es_client
        self.installed_fixtures: List[str] = []
    
    def setup_fixture(self, fixture: Fixture) -> bool:
        """Set up a single fixture."""
        # Delete existing index
        self.es_client.delete_index(fixture.index_name)
        
        # Create index with mappings
        if not self.es_client.create_index(fixture.index_name, fixture.mappings):
            return False
        
        # Index documents
        if fixture.documents:
            if not self.es_client.bulk_index(fixture.index_name, fixture.documents):
                return False
        
        # Refresh for immediate searchability
        self.es_client.refresh_index(fixture.index_name)
        
        self.installed_fixtures.append(fixture.index_name)
        return True
    
    def setup_all(self) -> bool:
        """Set up all fixtures."""
        fixtures = [
            self.get_logs_fixture(),
            self.get_metrics_fixture(),
            self.get_security_fixture(),
            self.get_apm_fixture(),
            self.get_search_fixture(),
        ]
        
        success = True
        for fixture in fixtures:
            if not self.setup_fixture(fixture):
                print(f"Failed to setup fixture: {fixture.name}")
                success = False
        
        return success
    
    def teardown_all(self) -> None:
        """Remove all installed fixtures."""
        for index_name in self.installed_fixtures:
            self.es_client.delete_index(index_name)
        self.installed_fixtures.clear()
    
    @staticmethod
    def _random_string(length: int = 8) -> str:
        return ''.join(random.choices(string.ascii_lowercase, k=length))
    
    @staticmethod
    def _random_ip() -> str:
        return f"{random.randint(1,255)}.{random.randint(0,255)}.{random.randint(0,255)}.{random.randint(1,255)}"
    
    @staticmethod
    def _random_timestamp(days_back: int = 7) -> str:
        dt = datetime.utcnow() - timedelta(
            days=random.randint(0, days_back),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59)
        )
        return dt.strftime("%Y-%m-%dT%H:%M:%S.000Z")
    
    def get_logs_fixture(self) -> Fixture:
        """Generate logs fixture for observability skills."""
        services = ['api-gateway', 'user-service', 'payment-service', 'notification-service', 'order-service']
        log_levels = ['DEBUG', 'INFO', 'WARN', 'ERROR']
        level_weights = [0.1, 0.6, 0.2, 0.1]
        
        trace_ids = [f"trace-{i:04d}" for i in range(20)]
        
        documents = []
        for i in range(200):
            level = random.choices(log_levels, level_weights)[0]
            service = random.choice(services)
            trace_id = random.choice(trace_ids)
            
            messages = {
                'DEBUG': f"Processing request for {self._random_string()}",
                'INFO': f"Request completed successfully",
                'WARN': f"High latency detected: {random.randint(500, 2000)}ms",
                'ERROR': f"Connection timeout to {random.choice(['database', 'cache', 'external-api'])}"
            }
            
            documents.append({
                '@timestamp': self._random_timestamp(),
                'log.level': level,
                'service.name': service,
                'message': messages[level],
                'trace.id': trace_id,
                'host.name': f"host-{random.randint(1, 5)}",
                'event.dataset': f"{service}.logs"
            })
        
        return Fixture(
            name="logs",
            index_name="logs-test",
            description="Sample application logs",
            mappings={
                'properties': {
                    '@timestamp': {'type': 'date'},
                    'log.level': {'type': 'keyword'},
                    'service.name': {'type': 'keyword'},
                    'message': {'type': 'text'},
                    'trace.id': {'type': 'keyword'},
                    'host.name': {'type': 'keyword'},
                    'event.dataset': {'type': 'keyword'}
                }
            },
            documents=documents
        )
    
    def get_metrics_fixture(self) -> Fixture:
        """Generate metrics fixture for infrastructure skills."""
        hosts = [f"host-{i:02d}" for i in range(10)]
        
        documents = []
        for host in hosts:
            for i in range(20):
                documents.append({
                    '@timestamp': self._random_timestamp(days_back=1),
                    'host.name': host,
                    'host.ip': self._random_ip(),
                    'system.cpu.total.pct': round(random.uniform(0.1, 0.95), 2),
                    'system.memory.used.pct': round(random.uniform(0.3, 0.9), 2),
                    'system.filesystem.used.pct': round(random.uniform(0.2, 0.85), 2),
                    'system.network.in.bytes': random.randint(1000000, 100000000),
                    'system.network.out.bytes': random.randint(1000000, 100000000),
                    'system.load.1': round(random.uniform(0.5, 4.0), 2),
                    'metricset.name': 'cpu'
                })
        
        return Fixture(
            name="metrics",
            index_name="metrics-test",
            description="System metrics data",
            mappings={
                'properties': {
                    '@timestamp': {'type': 'date'},
                    'host.name': {'type': 'keyword'},
                    'host.ip': {'type': 'ip'},
                    'system.cpu.total.pct': {'type': 'float'},
                    'system.memory.used.pct': {'type': 'float'},
                    'system.filesystem.used.pct': {'type': 'float'},
                    'system.network.in.bytes': {'type': 'long'},
                    'system.network.out.bytes': {'type': 'long'},
                    'system.load.1': {'type': 'float'},
                    'metricset.name': {'type': 'keyword'}
                }
            },
            documents=documents
        )
    
    def get_security_fixture(self) -> Fixture:
        """Generate security events fixture."""
        users = ['admin', 'user1', 'user2', 'service-account', 'attacker']
        event_types = ['authentication', 'authorization', 'file_access', 'network', 'process']
        outcomes = ['success', 'failure']
        
        threat_ips = ['192.168.1.100', '10.0.0.50', '172.16.0.200']
        normal_ips = [self._random_ip() for _ in range(20)]
        
        documents = []
        for i in range(150):
            user = random.choice(users)
            is_threat = user == 'attacker' or random.random() < 0.1
            
            documents.append({
                '@timestamp': self._random_timestamp(),
                'user.name': user,
                'user.id': f"uid-{hash(user) % 10000}",
                'event.category': random.choice(event_types),
                'event.outcome': 'failure' if is_threat and random.random() < 0.7 else random.choice(outcomes),
                'source.ip': random.choice(threat_ips if is_threat else normal_ips),
                'destination.ip': self._random_ip(),
                'host.name': f"server-{random.randint(1, 5)}",
                'process.name': random.choice(['sshd', 'nginx', 'mysql', 'python', 'bash']),
                'user.risk_score': random.randint(70, 100) if is_threat else random.randint(0, 40),
                'host.risk_score': random.randint(60, 95) if is_threat else random.randint(0, 30)
            })
        
        return Fixture(
            name="security",
            index_name="security-test",
            description="Security events data",
            mappings={
                'properties': {
                    '@timestamp': {'type': 'date'},
                    'user.name': {'type': 'keyword'},
                    'user.id': {'type': 'keyword'},
                    'event.category': {'type': 'keyword'},
                    'event.outcome': {'type': 'keyword'},
                    'source.ip': {'type': 'ip'},
                    'destination.ip': {'type': 'ip'},
                    'host.name': {'type': 'keyword'},
                    'process.name': {'type': 'keyword'},
                    'user.risk_score': {'type': 'integer'},
                    'host.risk_score': {'type': 'integer'}
                }
            },
            documents=documents
        )
    
    def get_apm_fixture(self) -> Fixture:
        """Generate APM traces fixture."""
        services = ['frontend', 'api-gateway', 'user-service', 'payment-service', 'database']
        transaction_types = ['request', 'worker', 'scheduled']
        
        trace_ids = [f"trace-{self._random_string(16)}" for _ in range(30)]
        
        documents = []
        for i in range(100):
            service = random.choice(services)
            is_slow = random.random() < 0.15
            is_error = random.random() < 0.1
            
            duration_ms = random.randint(800, 5000) if is_slow else random.randint(10, 300)
            
            documents.append({
                '@timestamp': self._random_timestamp(),
                'service.name': service,
                'service.environment': random.choice(['production', 'staging']),
                'transaction.name': f"/{random.choice(['api', 'users', 'orders', 'payments'])}/{random.choice(['get', 'post', 'list'])}",
                'transaction.type': random.choice(transaction_types),
                'transaction.duration.us': duration_ms * 1000,
                'transaction.result': 'HTTP 5xx' if is_error else 'HTTP 2xx',
                'transaction.outcome': 'failure' if is_error else 'success',
                'trace.id': random.choice(trace_ids),
                'span.id': self._random_string(16),
                'error.count': random.randint(1, 5) if is_error else 0
            })
        
        return Fixture(
            name="apm",
            index_name="apm-test",
            description="APM transaction traces",
            mappings={
                'properties': {
                    '@timestamp': {'type': 'date'},
                    'service.name': {'type': 'keyword'},
                    'service.environment': {'type': 'keyword'},
                    'transaction.name': {'type': 'keyword'},
                    'transaction.type': {'type': 'keyword'},
                    'transaction.duration.us': {'type': 'long'},
                    'transaction.result': {'type': 'keyword'},
                    'transaction.outcome': {'type': 'keyword'},
                    'trace.id': {'type': 'keyword'},
                    'span.id': {'type': 'keyword'},
                    'error.count': {'type': 'integer'}
                }
            },
            documents=documents
        )
    
    def get_search_fixture(self) -> Fixture:
        """Generate search content fixture."""
        categories = ['technology', 'science', 'business', 'health', 'sports']
        
        documents = []
        for i in range(50):
            category = random.choice(categories)
            
            titles = {
                'technology': f"New AI breakthrough in {random.choice(['machine learning', 'robotics', 'quantum computing'])}",
                'science': f"Discovery in {random.choice(['astronomy', 'biology', 'physics'])} research",
                'business': f"Market trends in {random.choice(['fintech', 'e-commerce', 'SaaS'])}",
                'health': f"Study reveals benefits of {random.choice(['exercise', 'diet', 'sleep'])}",
                'sports': f"Championship results for {random.choice(['football', 'basketball', 'tennis'])}"
            }
            
            documents.append({
                '@timestamp': self._random_timestamp(),
                'title': titles[category],
                'content': f"Lorem ipsum dolor sit amet, consectetur adipiscing elit. {self._random_string(50)}",
                'category': category,
                'author': f"author-{random.randint(1, 10)}",
                'views': random.randint(100, 10000),
                'tags': random.sample(['featured', 'trending', 'new', 'popular', 'editors-pick'], k=random.randint(1, 3))
            })
        
        return Fixture(
            name="search",
            index_name="content-test",
            description="Search content documents",
            mappings={
                'properties': {
                    '@timestamp': {'type': 'date'},
                    'title': {'type': 'text'},
                    'content': {'type': 'text'},
                    'category': {'type': 'keyword'},
                    'author': {'type': 'keyword'},
                    'views': {'type': 'integer'},
                    'tags': {'type': 'keyword'}
                }
            },
            documents=documents
        )
    
    def get_fixture_status(self) -> Dict[str, Any]:
        """Get status of all fixtures."""
        fixtures = ['logs-test', 'metrics-test', 'security-test', 'apm-test', 'content-test']
        status = {}
        
        for index in fixtures:
            exists = self.es_client.index_exists(index)
            count = self.es_client.count_documents(index) if exists else 0
            status[index] = {
                'exists': exists,
                'document_count': count
            }
        
        return status
