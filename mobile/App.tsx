import React, { useMemo, useState } from 'react';
import {
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { citations, politicians, stances, timeline, votes } from './src/data';
import type { Politician } from './src/types';

type Screen =
  | 'Search'
  | 'Profile'
  | 'Voting'
  | 'Issues'
  | 'Timeline'
  | 'Compare'
  | 'Saved'
  | 'Sources';

const screens: Screen[] = ['Search', 'Profile', 'Voting', 'Issues', 'Timeline', 'Compare', 'Saved', 'Sources'];

export default function App() {
  const [screen, setScreen] = useState<Screen>('Search');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Politician>(politicians[0]);
  const [compare, setCompare] = useState<Politician>(politicians[1]);
  const [saved, setSaved] = useState<string[]>([politicians[0].id]);

  const results = useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) return politicians;
    return politicians.filter((person) =>
      `${person.firstName} ${person.lastName} ${person.party} ${person.state} ${person.office}`
        .toLowerCase()
        .includes(value),
    );
  }, [query]);

  function toggleSaved(id: string) {
    setSaved((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" />
      <View style={styles.shell}>
        <Header />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.nav}>
          {screens.map((item) => (
            <Pressable
              key={item}
              onPress={() => setScreen(item)}
              style={[styles.navButton, screen === item && styles.navButtonActive]}
            >
              <Text style={[styles.navText, screen === item && styles.navTextActive]}>{item}</Text>
            </Pressable>
          ))}
        </ScrollView>

        <ScrollView contentContainerStyle={styles.content}>
          {screen === 'Search' && (
            <SearchScreen
              query={query}
              setQuery={setQuery}
              results={results}
              selected={selected}
              onSelect={(person) => {
                setSelected(person);
                setScreen('Profile');
              }}
            />
          )}
          {screen === 'Profile' && <ProfileScreen politician={selected} saved={saved.includes(selected.id)} onSave={() => toggleSaved(selected.id)} />}
          {screen === 'Voting' && <VotingScreen />}
          {screen === 'Issues' && <IssueScreen />}
          {screen === 'Timeline' && <TimelineScreen />}
          {screen === 'Compare' && <CompareScreen selected={selected} compare={compare} setCompare={setCompare} />}
          {screen === 'Saved' && <SavedScreen savedIds={saved} onSelect={(person) => { setSelected(person); setScreen('Profile'); }} />}
          {screen === 'Sources' && <SourcesScreen />}
        </ScrollView>
      </View>
    </SafeAreaView>
  );
}

function Header() {
  return (
    <View style={styles.header}>
      <View style={styles.shield}><Text style={styles.shieldText}>PR</Text></View>
      <View>
        <Text style={styles.brand}>Public Record</Text>
        <Text style={styles.brandSub}>Voter accountability app</Text>
      </View>
    </View>
  );
}

function SearchScreen({
  query,
  setQuery,
  results,
  selected,
  onSelect,
}: {
  query: string;
  setQuery: (value: string) => void;
  results: Politician[];
  selected: Politician;
  onSelect: (person: Politician) => void;
}) {
  return (
    <View style={styles.stack}>
      <Text style={styles.label}>Search politicians</Text>
      <TextInput
        value={query}
        onChangeText={setQuery}
        placeholder="Name, party, state, office"
        placeholderTextColor="#7c8b84"
        style={styles.input}
      />
      {results.map((person) => (
        <Pressable
          key={person.id}
          onPress={() => onSelect(person)}
          style={[styles.personRow, selected.id === person.id && styles.personRowActive]}
        >
          <Avatar person={person} />
          <View style={styles.flex}>
            <Text style={styles.rowTitle}>{person.firstName} {person.lastName}</Text>
            <Text style={styles.rowSub}>{person.office} · {person.state}</Text>
          </View>
        </Pressable>
      ))}
    </View>
  );
}

function ProfileScreen({ politician, saved, onSave }: { politician: Politician; saved: boolean; onSave: () => void }) {
  return (
    <View style={styles.stack}>
      <Card>
        <View style={styles.profileTop}>
          <Avatar person={politician} large />
          <View style={styles.flex}>
            <Text style={styles.eyebrow}>{politician.party} · {politician.state}</Text>
            <Text style={styles.title}>{politician.firstName} {politician.lastName}</Text>
            <Text style={styles.muted}>{politician.office}</Text>
          </View>
        </View>
        <Text style={styles.body}>{politician.biography}</Text>
        <Pressable onPress={onSave} style={styles.primaryButton}>
          <Text style={styles.primaryButtonText}>{saved ? 'Saved' : 'Save politician'}</Text>
        </Pressable>
      </Card>

      <View style={styles.metricRow}>
        <Metric label="Trust avg" value="82%" good />
        <Metric label="Citations" value="41" />
        <Metric label="Open risks" value="3" warn />
      </View>
    </View>
  );
}

function VotingScreen() {
  return (
    <View style={styles.stack}>
      <View style={styles.metricRow}>
        <Metric label="Supported" value="2" good />
        <Metric label="Opposed" value="1" warn />
        <Metric label="Abstained" value="0" />
      </View>
      {votes.map((vote) => (
        <Card key={vote.id}>
          <View style={styles.split}>
            <Text style={styles.rowTitle}>{vote.billNumber}</Text>
            <Badge label={vote.vote} tone={vote.vote === 'YEA' ? 'good' : 'warn'} />
          </View>
          <Text style={styles.body}>{vote.title}</Text>
          <Text style={styles.muted}>{vote.date}</Text>
        </Card>
      ))}
    </View>
  );
}

function IssueScreen() {
  return (
    <View style={styles.stack}>
      {stances.map((stance) => (
        <Card key={stance.issue}>
          <View style={styles.split}>
            <Text style={styles.rowTitle}>{stance.issue}</Text>
            <Badge label={stance.confidence} tone={stance.confidence === 'High' ? 'good' : 'neutral'} />
          </View>
          <Text style={styles.body}>{stance.stance}</Text>
          <Text style={styles.muted}>{stance.basis}</Text>
        </Card>
      ))}
    </View>
  );
}

function TimelineScreen() {
  return (
    <View style={styles.stack}>
      {timeline.map((item) => (
        <Card key={item.id}>
          <Text style={styles.eyebrow}>{item.date} · {item.category}</Text>
          <Text style={styles.rowTitle}>{item.title}</Text>
          <Text style={styles.body}>{item.detail}</Text>
        </Card>
      ))}
    </View>
  );
}

function CompareScreen({
  selected,
  compare,
  setCompare,
}: {
  selected: Politician;
  compare: Politician;
  setCompare: (person: Politician) => void;
}) {
  return (
    <View style={styles.stack}>
      <Text style={styles.label}>Compare with</Text>
      <View style={styles.optionRow}>
        {politicians.map((person) => (
          <Pressable
            key={person.id}
            onPress={() => setCompare(person)}
            style={[styles.compareChoice, compare.id === person.id && styles.compareChoiceActive]}
          >
            <Text style={styles.compareText}>{person.firstName} {person.lastName}</Text>
          </Pressable>
        ))}
      </View>
      <View style={styles.compareGrid}>
        <CompareColumn person={selected} />
        <CompareColumn person={compare} />
      </View>
    </View>
  );
}

function CompareColumn({ person }: { person: Politician }) {
  return (
    <Card style={styles.compareColumn}>
      <Avatar person={person} />
      <Text style={styles.rowTitle}>{person.firstName} {person.lastName}</Text>
      <Text style={styles.muted}>{person.party} · {person.state}</Text>
      <Text style={styles.body}>{person.office}</Text>
      <Metric label="Trust avg" value={person.id.endsWith('0') ? '82%' : '76%'} good />
    </Card>
  );
}

function SavedScreen({ savedIds, onSelect }: { savedIds: string[]; onSelect: (person: Politician) => void }) {
  const saved = politicians.filter((person) => savedIds.includes(person.id));
  return (
    <View style={styles.stack}>
      {saved.map((person) => (
        <Pressable key={person.id} onPress={() => onSelect(person)} style={styles.personRow}>
          <Avatar person={person} />
          <View style={styles.flex}>
            <Text style={styles.rowTitle}>{person.firstName} {person.lastName}</Text>
            <Text style={styles.rowSub}>{person.office} · {person.state}</Text>
          </View>
        </Pressable>
      ))}
      {saved.length === 0 && <Text style={styles.body}>No saved politicians yet.</Text>}
    </View>
  );
}

function SourcesScreen() {
  return (
    <View style={styles.stack}>
      <Card>
        <Text style={styles.rowTitle}>Integrity model</Text>
        <Text style={styles.body}>
          Verified facts, quotes, votes, allegations, opinion, and unresolved claims are separated before display.
        </Text>
      </Card>
      {citations.map((citation) => (
        <Card key={citation.id}>
          <View style={styles.split}>
            <Text style={styles.rowTitle}>{citation.source}</Text>
            <Badge label={citation.quality} tone="good" />
          </View>
          <Text style={styles.muted}>{citation.date}</Text>
          <Text style={styles.body}>{citation.url}</Text>
        </Card>
      ))}
    </View>
  );
}

function Avatar({ person, large }: { person: Politician; large?: boolean }) {
  return (
    <View style={[styles.avatar, large && styles.avatarLarge]}>
      <Text style={styles.avatarText}>{person.firstName[0]}{person.lastName[0]}</Text>
    </View>
  );
}

function Card({ children, style }: { children: React.ReactNode; style?: object }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

function Metric({ label, value, good, warn }: { label: string; value: string; good?: boolean; warn?: boolean }) {
  return (
    <View style={styles.metric}>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={[styles.metricValue, good && styles.goodText, warn && styles.warnText]}>{value}</Text>
    </View>
  );
}

function Badge({ label, tone }: { label: string; tone: 'good' | 'warn' | 'neutral' }) {
  return (
    <View style={[styles.badge, tone === 'good' && styles.badgeGood, tone === 'warn' && styles.badgeWarn]}>
      <Text style={styles.badgeText}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#13201b',
  },
  shell: {
    flex: 1,
    backgroundColor: '#f4f7f5',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 18,
    backgroundColor: '#13201b',
  },
  shield: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 20,
    backgroundColor: '#c7ead4',
  },
  shieldText: {
    color: '#13201b',
    fontWeight: '900',
  },
  brand: {
    color: '#f8fbf8',
    fontSize: 20,
    fontWeight: '800',
  },
  brandSub: {
    color: '#aebdb4',
  },
  nav: {
    maxHeight: 58,
    backgroundColor: '#13201b',
    paddingHorizontal: 12,
  },
  navButton: {
    height: 40,
    justifyContent: 'center',
    paddingHorizontal: 14,
    marginRight: 8,
    borderRadius: 8,
  },
  navButtonActive: {
    backgroundColor: '#c7ead4',
  },
  navText: {
    color: '#dce6df',
    fontWeight: '700',
  },
  navTextActive: {
    color: '#13201b',
  },
  content: {
    padding: 16,
    paddingBottom: 36,
  },
  stack: {
    gap: 12,
  },
  label: {
    color: '#34463d',
    fontWeight: '800',
  },
  input: {
    height: 48,
    paddingHorizontal: 14,
    color: '#1d252c',
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d9e1dc',
    borderRadius: 8,
  },
  personRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 12,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d9e1dc',
    borderRadius: 8,
  },
  personRowActive: {
    borderColor: '#78aa8b',
    backgroundColor: '#edf7f0',
  },
  avatar: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 22,
    backgroundColor: '#b8dfca',
  },
  avatarLarge: {
    width: 66,
    height: 66,
    borderRadius: 33,
  },
  avatarText: {
    color: '#102017',
    fontSize: 16,
    fontWeight: '900',
  },
  flex: {
    flex: 1,
  },
  rowTitle: {
    color: '#1d252c',
    fontSize: 16,
    fontWeight: '800',
  },
  rowSub: {
    color: '#66786f',
    marginTop: 2,
  },
  card: {
    gap: 10,
    padding: 14,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d9e1dc',
    borderRadius: 8,
  },
  profileTop: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
  },
  eyebrow: {
    color: '#527163',
    fontSize: 12,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  title: {
    color: '#1d252c',
    fontSize: 28,
    fontWeight: '900',
  },
  muted: {
    color: '#607168',
  },
  body: {
    color: '#43524b',
    lineHeight: 21,
  },
  primaryButton: {
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
    backgroundColor: '#13201b',
  },
  primaryButtonText: {
    color: '#f8fbf8',
    fontWeight: '800',
  },
  metricRow: {
    flexDirection: 'row',
    gap: 10,
  },
  metric: {
    flex: 1,
    padding: 12,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d9e1dc',
    borderRadius: 8,
  },
  metricLabel: {
    color: '#66786f',
    fontSize: 12,
  },
  metricValue: {
    marginTop: 3,
    color: '#1d252c',
    fontSize: 24,
    fontWeight: '900',
  },
  goodText: {
    color: '#187048',
  },
  warnText: {
    color: '#a14c20',
  },
  split: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
  },
  badge: {
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 999,
    backgroundColor: '#e7edf0',
  },
  badgeGood: {
    backgroundColor: '#e4f4eb',
  },
  badgeWarn: {
    backgroundColor: '#fff1d3',
  },
  badgeText: {
    color: '#1d252c',
    fontSize: 12,
    fontWeight: '800',
  },
  optionRow: {
    flexDirection: 'row',
    gap: 8,
  },
  compareChoice: {
    flex: 1,
    padding: 10,
    borderRadius: 8,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d9e1dc',
  },
  compareChoiceActive: {
    borderColor: '#78aa8b',
    backgroundColor: '#edf7f0',
  },
  compareText: {
    color: '#1d252c',
    fontWeight: '800',
  },
  compareGrid: {
    flexDirection: 'row',
    gap: 10,
  },
  compareColumn: {
    flex: 1,
  },
});
