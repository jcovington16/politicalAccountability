import React, { useMemo, useState } from 'react';
import {
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { articles, bills, billVotes, citations, politicians, stances, timeline, votes } from './src/data';
import type { Bill, Politician } from './src/types';

type MainScreen = 'Search' | 'Bills' | 'Profile' | 'Saved' | 'BillDetail';
type ProfileTab = 'Overview' | 'Votes' | 'Bills' | 'Articles' | 'Issues' | 'Timeline' | 'Compare' | 'Citations';

const mainScreens: MainScreen[] = ['Search', 'Bills', 'Saved'];
const profileTabs: ProfileTab[] = ['Overview', 'Votes', 'Bills', 'Articles', 'Issues', 'Timeline', 'Compare', 'Citations'];

export default function App() {
  const [screen, setScreen] = useState<MainScreen>('Search');
  const [profileTab, setProfileTab] = useState<ProfileTab>('Overview');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Politician>(politicians[0]);
  const [selectedBill, setSelectedBill] = useState<Bill>(bills[0]);
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

  const billResults = useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) return bills;
    return bills.filter((bill) =>
      `${bill.billNumber} ${bill.title} ${bill.description} ${bill.sponsor} ${bill.status}`
        .toLowerCase()
        .includes(value),
    );
  }, [query]);

  function toggleSaved(id: string) {
    setSaved((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));
  }

  const appContent = (
    <View style={[styles.shell, Platform.OS === 'web' && styles.webShell]}>
      <Header />
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.nav}>
        {mainScreens.map((item) => (
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
              setProfileTab('Overview');
              setScreen('Profile');
            }}
          />
        )}
        {screen === 'Profile' && (
          <PoliticianPage
            politician={selected}
            saved={saved.includes(selected.id)}
            onSave={() => toggleSaved(selected.id)}
            activeTab={profileTab}
            setActiveTab={setProfileTab}
            compare={compare}
            setCompare={setCompare}
            onBack={() => setScreen('Search')}
            onOpenBill={(bill) => {
              setSelectedBill(bill);
              setScreen('BillDetail');
            }}
          />
        )}
        {screen === 'Bills' && (
          <BillSearchScreen
            query={query}
            setQuery={setQuery}
            results={billResults}
            onOpenBill={(bill) => {
              setSelectedBill(bill);
              setScreen('BillDetail');
            }}
          />
        )}
        {screen === 'BillDetail' && (
          <BillDetailScreen
            bill={selectedBill}
            onBack={() => setScreen('Bills')}
            onOpenPolitician={(person) => {
              setSelected(person);
              setProfileTab('Overview');
              setScreen('Profile');
            }}
          />
        )}
        {screen === 'Saved' && <SavedScreen savedIds={saved} onSelect={(person) => { setSelected(person); setProfileTab('Overview'); setScreen('Profile'); }} />}
      </ScrollView>
    </View>
  );

  if (Platform.OS === 'web') {
    return (
      <View style={styles.webStage}>
        <View style={styles.deviceFrame}>
          <View style={styles.deviceNotch} />
          {appContent}
          <View style={styles.deviceHomeIndicator} />
        </View>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" />
      {appContent}
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
      <Card style={styles.searchHero}>
        <Text style={styles.eyebrow}>Neutral public record</Text>
        <Text style={styles.heroTitle}>Search who represents you.</Text>
        <Text style={styles.body}>
          Find state and federal politicians, then review what they voted for, sponsored, said, and had reported about them.
        </Text>
      </Card>
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

function BillSearchScreen({
  query,
  setQuery,
  results,
  onOpenBill,
}: {
  query: string;
  setQuery: (value: string) => void;
  results: Bill[];
  onOpenBill: (bill: Bill) => void;
}) {
  return (
    <View style={styles.stack}>
      <Card style={styles.searchHero}>
        <Text style={styles.eyebrow}>Legislation lookup</Text>
        <Text style={styles.heroTitle}>Search bills and votes.</Text>
        <Text style={styles.body}>
          Look up a bill, see who introduced it, when it moved, and how politicians voted.
        </Text>
      </Card>
      <Text style={styles.label}>Search bills</Text>
      <TextInput
        value={query}
        onChangeText={setQuery}
        placeholder="Bill number, title, sponsor, topic"
        placeholderTextColor="#7c8b84"
        style={styles.input}
      />
      {results.map((bill) => (
        <BillRow key={bill.id} bill={bill} onOpenBill={onOpenBill} />
      ))}
    </View>
  );
}

function BillRow({ bill, onOpenBill }: { bill: Bill; onOpenBill: (bill: Bill) => void }) {
  return (
    <Pressable onPress={() => onOpenBill(bill)} style={styles.personRow}>
      <View style={styles.billIcon}><Text style={styles.billIconText}>{bill.billNumber.split('-')[0]}</Text></View>
      <View style={styles.flex}>
        <View style={styles.split}>
          <Text style={styles.rowTitle}>{bill.billNumber}</Text>
          <Badge label={bill.status} tone={bill.status === 'Passed' ? 'good' : 'neutral'} />
        </View>
        <Text style={styles.rowSub}>{bill.title}</Text>
        <Text style={styles.muted}>Sponsor: {bill.sponsor}</Text>
      </View>
    </Pressable>
  );
}

function BillDetailScreen({
  bill,
  onBack,
  onOpenPolitician,
}: {
  bill: Bill;
  onBack: () => void;
  onOpenPolitician: (person: Politician) => void;
}) {
  const votesForBill = billVotes[bill.id] ?? [];
  const yea = votesForBill.filter((vote) => vote.voteType === 'YEA');
  const nay = votesForBill.filter((vote) => vote.voteType === 'NAY');
  const abstain = votesForBill.filter((vote) => vote.voteType === 'ABSTAIN');

  return (
    <View style={styles.stack}>
      <Pressable onPress={onBack} style={styles.textButton}>
        <Text style={styles.textButtonLabel}>Back to bills</Text>
      </Pressable>
      <Card>
        <Text style={styles.eyebrow}>{bill.jurisdiction} · {bill.chamber}</Text>
        <Text style={styles.title}>{bill.billNumber}</Text>
        <Text style={styles.rowTitle}>{bill.title}</Text>
        <Text style={styles.body}>{bill.description}</Text>
        <Badge label={bill.status} tone={bill.status === 'Passed' ? 'good' : 'neutral'} />
      </Card>
      <View style={styles.metricRow}>
        <Metric label="For" value={`${yea.length}`} good />
        <Metric label="Against" value={`${nay.length}`} warn />
        <Metric label="Abstain" value={`${abstain.length}`} />
      </View>
      <Card>
        <Text style={styles.rowTitle}>Legislation Details</Text>
        <Text style={styles.body}>Introduced by {bill.sponsor}</Text>
        <Text style={styles.body}>Introduced {bill.introducedDate}</Text>
        <Text style={styles.body}>Last action {bill.lastActionDate ?? 'Unknown'}</Text>
        <Text style={styles.body}>{bill.billUrl}</Text>
      </Card>
      <Text style={styles.label}>Votes For</Text>
      {yea.map((vote) => <BillVoteRow key={vote.id} vote={vote} onOpenPolitician={onOpenPolitician} />)}
      <Text style={styles.label}>Votes Against</Text>
      {nay.map((vote) => <BillVoteRow key={vote.id} vote={vote} onOpenPolitician={onOpenPolitician} />)}
      {abstain.length > 0 && <Text style={styles.label}>Abstained</Text>}
      {abstain.map((vote) => <BillVoteRow key={vote.id} vote={vote} onOpenPolitician={onOpenPolitician} />)}
    </View>
  );
}

function BillVoteRow({
  vote,
  onOpenPolitician,
}: {
  vote: {
    politicianId: string;
    politicianName: string;
    party: string;
    state: string;
    voteType: 'YEA' | 'NAY' | 'ABSTAIN';
    voteDate: string;
  };
  onOpenPolitician: (person: Politician) => void;
}) {
  const politician = politicians.find((person) => person.id === vote.politicianId);

  return (
    <Pressable disabled={!politician} onPress={() => politician && onOpenPolitician(politician)}>
    <Card>
      <View style={styles.split}>
        <Text style={styles.rowTitle}>{vote.politicianName}</Text>
        <Badge label={vote.voteType} tone={vote.voteType === 'YEA' ? 'good' : vote.voteType === 'NAY' ? 'warn' : 'neutral'} />
      </View>
      <Text style={styles.muted}>{vote.party} · {vote.state} · {vote.voteDate}</Text>
    </Card>
    </Pressable>
  );
}

function PoliticianPage({
  politician,
  saved,
  onSave,
  activeTab,
  setActiveTab,
  compare,
  setCompare,
  onBack,
  onOpenBill,
}: {
  politician: Politician;
  saved: boolean;
  onSave: () => void;
  activeTab: ProfileTab;
  setActiveTab: (tab: ProfileTab) => void;
  compare: Politician;
  setCompare: (person: Politician) => void;
  onBack: () => void;
  onOpenBill: (bill: Bill) => void;
}) {
  return (
    <View style={styles.stack}>
      <Pressable onPress={onBack} style={styles.textButton}>
        <Text style={styles.textButtonLabel}>Back to search</Text>
      </Pressable>
      <ProfileHeader politician={politician} saved={saved} onSave={onSave} />
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.profileTabs}>
        {profileTabs.map((tab) => (
          <Pressable
            key={tab}
            onPress={() => setActiveTab(tab)}
            style={[styles.profileTabButton, activeTab === tab && styles.profileTabButtonActive]}
          >
            <Text style={[styles.profileTabText, activeTab === tab && styles.profileTabTextActive]}>{tab}</Text>
          </Pressable>
        ))}
      </ScrollView>
      {activeTab === 'Overview' && <ProfileScreen politician={politician} />}
      {activeTab === 'Votes' && <VotingScreen onOpenBill={onOpenBill} />}
      {activeTab === 'Bills' && <BillsScreen onOpenBill={onOpenBill} />}
      {activeTab === 'Articles' && <ArticlesScreen />}
      {activeTab === 'Issues' && <IssueScreen />}
      {activeTab === 'Timeline' && <TimelineScreen />}
      {activeTab === 'Compare' && <CompareScreen selected={politician} compare={compare} setCompare={setCompare} />}
      {activeTab === 'Citations' && <SourcesScreen />}
    </View>
  );
}

function ProfileHeader({ politician, saved, onSave }: { politician: Politician; saved: boolean; onSave: () => void }) {
  return (
    <Card>
      <View style={styles.profileTop}>
        <Avatar person={politician} large />
        <View style={styles.flex}>
          <Text style={styles.eyebrow}>{politician.party} · {politician.state}</Text>
          <Text style={styles.title}>{politician.firstName} {politician.lastName}</Text>
          <Text style={styles.muted}>{politician.office}</Text>
        </View>
      </View>
      <Pressable onPress={onSave} style={styles.primaryButton}>
        <Text style={styles.primaryButtonText}>{saved ? 'Saved' : 'Save politician'}</Text>
      </Pressable>
    </Card>
  );
}

function ProfileScreen({ politician }: { politician: Politician }) {
  return (
    <View style={styles.stack}>
      <Card>
        <Text style={styles.rowTitle}>Biography</Text>
        <Text style={styles.body}>{politician.biography}</Text>
      </Card>

      <View style={styles.metricRow}>
        <Metric label="Trust avg" value="82%" good />
        <Metric label="Citations" value="41" />
        <Metric label="Open risks" value="3" warn />
      </View>
    </View>
  );
}

function BillsScreen({ onOpenBill }: { onOpenBill: (bill: Bill) => void }) {
  const supported = votes.filter((vote) => vote.voteType === 'YEA');
  const opposed = votes.filter((vote) => vote.voteType === 'NAY');
  return (
    <View style={styles.stack}>
      <Text style={styles.label}>Supported</Text>
      {supported.map((vote) => (
        <Pressable key={vote.id} onPress={() => onOpenBill(bills.find((bill) => bill.id === vote.billId) ?? bills[0])}>
        <Card>
          <Text style={styles.rowTitle}>{vote.billNumber}</Text>
          <Text style={styles.body}>{vote.billTitle}</Text>
        </Card>
        </Pressable>
      ))}
      <Text style={styles.label}>Opposed</Text>
      {opposed.map((vote) => (
        <Pressable key={vote.id} onPress={() => onOpenBill(bills.find((bill) => bill.id === vote.billId) ?? bills[0])}>
        <Card>
          <Text style={styles.rowTitle}>{vote.billNumber}</Text>
          <Text style={styles.body}>{vote.billTitle}</Text>
        </Card>
        </Pressable>
      ))}
    </View>
  );
}

function ArticlesScreen() {
  return (
    <View style={styles.stack}>
      {articles.map((article) => (
        <Card key={article.id}>
          <View style={styles.split}>
            <Text style={styles.rowTitle}>{article.headline}</Text>
            <Badge label={article.tone} tone={article.tone === 'Controversy' ? 'warn' : 'neutral'} />
          </View>
          <Text style={styles.muted}>{article.source} · {article.date}</Text>
          <Text style={styles.body}>{article.summary}</Text>
        </Card>
      ))}
    </View>
  );
}

function VotingScreen({ onOpenBill }: { onOpenBill: (bill: Bill) => void }) {
  return (
    <View style={styles.stack}>
      <View style={styles.metricRow}>
        <Metric label="Supported" value="2" good />
        <Metric label="Opposed" value="1" warn />
        <Metric label="Abstained" value="0" />
      </View>
      {votes.map((vote) => (
        <Pressable key={vote.id} onPress={() => onOpenBill(bills.find((bill) => bill.id === vote.billId) ?? bills[0])}>
        <Card>
          <View style={styles.split}>
            <Text style={styles.rowTitle}>{vote.billNumber}</Text>
            <Badge label={vote.voteType} tone={vote.voteType === 'YEA' ? 'good' : 'warn'} />
          </View>
          <Text style={styles.body}>{vote.billTitle}</Text>
          <Text style={styles.muted}>{vote.voteDate}</Text>
        </Card>
        </Pressable>
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
    backgroundColor: '#e8eee9',
  },
  webStage: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 18,
    backgroundColor: '#e8eee9',
  },
  deviceFrame: {
    position: 'relative',
    width: '100%',
    maxWidth: 430,
    height: '94%',
    maxHeight: 932,
    minHeight: 680,
    paddingTop: 18,
    paddingRight: 10,
    paddingBottom: 18,
    paddingLeft: 10,
    borderWidth: 10,
    borderColor: '#07100d',
    borderRadius: 46,
    backgroundColor: '#07100d',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 18 },
    shadowOpacity: 0.28,
    shadowRadius: 30,
  },
  deviceNotch: {
    position: 'absolute',
    top: 10,
    left: '50%',
    width: 128,
    height: 28,
    marginLeft: -64,
    borderBottomLeftRadius: 16,
    borderBottomRightRadius: 16,
    backgroundColor: '#07100d',
    zIndex: 10,
  },
  deviceHomeIndicator: {
    position: 'absolute',
    bottom: 8,
    left: '50%',
    width: 118,
    height: 4,
    marginLeft: -59,
    borderRadius: 2,
    backgroundColor: '#d9e1dc',
    opacity: 0.85,
  },
  shell: {
    flex: 1,
    width: '100%',
    maxWidth: 480,
    alignSelf: 'center',
    backgroundColor: '#f4f7f5',
    overflow: 'hidden',
  },
  webShell: {
    borderRadius: 34,
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
  searchHero: {
    backgroundColor: '#e8f4ed',
  },
  heroTitle: {
    color: '#13201b',
    fontSize: 25,
    fontWeight: '900',
  },
  profileTabs: {
    maxHeight: 52,
  },
  profileTabButton: {
    height: 38,
    justifyContent: 'center',
    paddingHorizontal: 12,
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#d9e1dc',
    borderRadius: 8,
    backgroundColor: '#ffffff',
  },
  profileTabButtonActive: {
    borderColor: '#78aa8b',
    backgroundColor: '#13201b',
  },
  profileTabText: {
    color: '#34463d',
    fontWeight: '800',
  },
  profileTabTextActive: {
    color: '#f8fbf8',
  },
  textButton: {
    alignSelf: 'flex-start',
    paddingVertical: 4,
  },
  textButtonLabel: {
    color: '#187048',
    fontWeight: '800',
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
  billIcon: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
    backgroundColor: '#13201b',
  },
  billIconText: {
    color: '#c7ead4',
    fontSize: 12,
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
