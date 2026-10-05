package com.yourteam.lostfound.config;

import com.yourteam.lostfound.model.Claim;
import com.yourteam.lostfound.model.FoundItem;
import com.yourteam.lostfound.model.LostItem;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.repository.ClaimRepository;
import com.yourteam.lostfound.repository.ItemRepository;
import com.yourteam.lostfound.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DataInitializer seeds the curated 12-item demonstration catalog and default campus accounts
 * whenever connecting to a clean database (e.g. Supabase PostgreSQL or clean local H2).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ItemRepository itemRepository,
            ClaimRepository claimRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Checking database state for initial demo seed...");

        // Guarantee Admin user exists
        User admin = userRepository.findByEmail("admin@lostfound.edu").orElseGet(() -> {
            User u = new User("Campus Admin", "admin@lostfound.edu", passwordEncoder.encode("Admin@123"), "ROLE_ADMIN");
            u.setPhoneNumber("555-0199");
            return userRepository.save(u);
        });

        // Demo student users
        User rohan = userRepository.findByEmail("rohan@student.edu").orElseGet(() -> {
            User u = new User("Rohan Sharma", "rohan@student.edu", passwordEncoder.encode("Password@123"), "ROLE_USER");
            u.setPhoneNumber("555-0101");
            return userRepository.save(u);
        });

        User abinav = userRepository.findByEmail("abinav@lostfound.edu").orElseGet(() -> {
            User u = new User("Abinav Sabu", "abinav@lostfound.edu", passwordEncoder.encode("Password@123"), "ROLE_USER");
            u.setPhoneNumber("555-0102");
            return userRepository.save(u);
        });

        User david = userRepository.findByEmail("david@student.edu").orElseGet(() -> {
            User u = new User("David Miller", "david@student.edu", passwordEncoder.encode("Password@123"), "ROLE_USER");
            u.setPhoneNumber("555-0103");
            return userRepository.save(u);
        });

        User sneha = userRepository.findByEmail("sneha@student.edu").orElseGet(() -> {
            User u = new User("Sneha Patel", "sneha@student.edu", passwordEncoder.encode("Password@123"), "ROLE_USER");
            u.setPhoneNumber("555-0104");
            return userRepository.save(u);
        });

        // Only seed demonstration catalog if database has zero items
        if (itemRepository.count() == 0) {
            log.info("Database has 0 items. Seeding complete 12-item curated demonstration catalog...");

            // 1. iPhone 15 Pro (LOST)
            LostItem item1 = new LostItem(
                    "Apple iPhone 15 Pro (Natural Titanium)",
                    "Apple iPhone 15 Pro 128GB in Natural Titanium with clear MagSafe case and small hairline scratch on upper bezel.",
                    "ELECTRONICS",
                    LocalDate.now().minusDays(3),
                    "Student Union - 2nd Floor Lounge",
                    "LOST",
                    "$50 Reward"
            );
            item1.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8001.jpg");
            item1.setLatitude(37.7749);
            item1.setLongitude(-122.4194);
            item1.setVerificationQuestion("What is the lock screen wallpaper picture?");
            item1.setUser(rohan);

            // 2. MacBook Air M2 (FOUND)
            FoundItem item2 = new FoundItem(
                    "Apple MacBook Air M2 (Space Gray)",
                    "Space Gray 13-inch MacBook Air M2 found on silent study desk 14. Kept in navy neoprene protective sleeve.",
                    "ELECTRONICS",
                    LocalDate.now().minusDays(2),
                    "Main Library - 2nd Floor Study Area",
                    "FOUND",
                    "Shelf 4B",
                    "Claim at Central Desk with matching serial number"
            );
            item2.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8002.jpg");
            item2.setLatitude(37.7752);
            item2.setLongitude(-122.4189);
            item2.setCustodyDesk("Library Central Circulation Desk");
            item2.setStorageBin("BIN-ELEC-402");
            item2.setVerificationQuestion("What color protective skin or stickers are on the keyboard deck?");
            item2.setUser(abinav);

            // 3. Wallet (CLAIMED)
            FoundItem item3 = new FoundItem(
                    "Navy Blue Leather Bi-Fold Wallet",
                    "Genuine navy blue leather bi-fold wallet by Tommy Hilfiger. Contains student identification and transit card.",
                    "WALLETS_CARDS",
                    LocalDate.now().minusDays(4),
                    "Indoor Sports Complex - Court 2",
                    "CLAIMED",
                    "Safe 1",
                    "Verify ID at Welcome Desk"
            );
            item3.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8003.jpg");
            item3.setLatitude(37.7740);
            item3.setLongitude(-122.4201);
            item3.setCustodyDesk("Sports Complex Welcome Desk");
            item3.setStorageBin("BIN-WALLET-104");
            item3.setVerificationQuestion("What is the name printed on the transit smartcard inside?");
            item3.setUser(abinav);

            // 4. North Face Backpack (LOST)
            LostItem item4 = new LostItem(
                    "The North Face Borealis Backpack (Black)",
                    "Black 28L North Face Borealis backpack with reflective bungee cords and side mesh water bottle pockets.",
                    "CLOTHING",
                    LocalDate.now().minusDays(3),
                    "Science Block B - Lecture Hall 101",
                    "LOST",
                    "$30 Reward"
            );
            item4.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8004.jpg");
            item4.setLatitude(37.7758);
            item4.setLongitude(-122.4175);
            item4.setVerificationQuestion("What textbook title was inside the main zipper pocket?");
            item4.setUser(david);

            // 5. Dorm & Bike Keys (FOUND)
            FoundItem item5 = new FoundItem(
                    "Set of Dorm & Bike Keys on Black Carabiner",
                    "Heavy duty matte black climbing carabiner holding 2 brass Yale dorm keys, 1 Kryptonite U-lock key, and red tag.",
                    "KEYS",
                    LocalDate.now().minusDays(2),
                    "Science Quad - Bicycle Racks",
                    "FOUND",
                    "Key Lockbox 2",
                    "Claim at Security Gate 1 with ID"
            );
            item5.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8005.jpg");
            item5.setLatitude(37.7761);
            item5.setLongitude(-122.4178);
            item5.setCustodyDesk("Campus Security Office (Gate 1)");
            item5.setStorageBin("BIN-KEYS-105");
            item5.setVerificationQuestion("What text or logo is engraved on the red tag?");
            item5.setUser(abinav);

            // 6. Apple Watch Series 9 (REUNITED)
            FoundItem item6 = new FoundItem(
                    "Apple Watch Series 9 (Midnight Aluminum 45mm)",
                    "45mm Apple Watch Series 9 GPS in Midnight aluminum casing with black sport band. Turned in after evening gym workout.",
                    "ELECTRONICS",
                    LocalDate.now().minusDays(6),
                    "Campus Recreation Center - Weight Room",
                    "REUNITED",
                    "Locker 3",
                    "Pair to your phone at front desk"
            );
            item6.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8006.jpg");
            item6.setLatitude(37.7738);
            item6.setLongitude(-122.4190);
            item6.setCustodyDesk("Recreation Center Front Desk");
            item6.setStorageBin("BIN-ELEC-310");
            item6.setVerificationQuestion("What Bluetooth device name is displayed under Settings?");
            item6.setUser(sneha);

            // 7. Sony Headphones (FOUND)
            FoundItem item7 = new FoundItem(
                    "Sony WH-1000XM5 Wireless Noise-Cancelling Headphones",
                    "Silver/platinum over-ear Sony WH-1000XM5 noise cancelling headphones in matching zip carry case.",
                    "ELECTRONICS",
                    LocalDate.now().minusDays(2),
                    "Dining Hall - South Booths",
                    "FOUND",
                    "Locker A",
                    "Turn in at Info Booth"
            );
            item7.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8007.jpg");
            item7.setLatitude(37.7745);
            item7.setLongitude(-122.4182);
            item7.setCustodyDesk("Dining Hall Info Booth");
            item7.setStorageBin("BIN-AUDIO-118");
            item7.setVerificationQuestion("What auxiliary cord color is stored inside the case zipper pocket?");
            item7.setUser(abinav);

            // 8. Hydro Flask (LOST)
            LostItem item8 = new LostItem(
                    "Stainless Steel Hydro Flask (Olive Green 32oz)",
                    "32oz wide-mouth olive green Hydro Flask with flex cap, decorated with national parks stickers and NASA badge.",
                    "OTHER",
                    LocalDate.now().minusDays(3),
                    "University Lawn - Amphitheater",
                    "LOST",
                    "$15 Reward"
            );
            item8.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8008.jpg");
            item8.setLatitude(37.7750);
            item8.setLongitude(-122.4168);
            item8.setVerificationQuestion("Name two stickers placed beneath the logo?");
            item8.setUser(sneha);

            // 9. TI-84 Plus Calculator (FOUND)
            FoundItem item9 = new FoundItem(
                    "Texas Instruments TI-84 Plus CE Graphing Calculator",
                    "Dark blue TI-84 Plus CE graphing calculator with slide cover. Found on desk in calculus recitation classroom.",
                    "ELECTRONICS",
                    LocalDate.now().minusDays(2),
                    "Mathematics Hall - Room 204",
                    "FOUND",
                    "Cabinet 2",
                    "Claim at Math Dept office with student ID"
            );
            item9.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8009.jpg");
            item9.setLatitude(37.7765);
            item9.setLongitude(-122.4185);
            item9.setCustodyDesk("Math Department Office (Room 101)");
            item9.setStorageBin("BIN-ELEC-219");
            item9.setVerificationQuestion("What initials are etched inside the slide cover?");
            item9.setUser(abinav);

            // 10. Student ID & Access Smartcard (CLAIMED)
            FoundItem item10 = new FoundItem(
                    "Campus Student ID & Access Smartcard",
                    "Official University RFID campus access smartcard on blue lanyard with student barcode on reverse.",
                    "WALLETS_CARDS",
                    LocalDate.now().minusDays(4),
                    "Engineering Complex - Atrium",
                    "CLAIMED",
                    "Drawer 1",
                    "Claim with another photo ID"
            );
            item10.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8010.jpg");
            item10.setLatitude(37.7755);
            item10.setLongitude(-122.4170);
            item10.setCustodyDesk("Engineering Student Helpdesk");
            item10.setStorageBin("BIN-CARD-055");
            item10.setVerificationQuestion("What is the last 4 digits of the card barcode?");
            item10.setUser(david);

            // 11. Travel Umbrella (LOST)
            LostItem item11 = new LostItem(
                    "Compact Windproof Travel Umbrella (Navy Blue)",
                    "Navy blue compact folding umbrella with automatic open/close button and wooden curved handle grip.",
                    "OTHER",
                    LocalDate.now().minusDays(3),
                    "Humanities Building - 1st Floor Lobby",
                    "LOST",
                    "$10 Reward"
            );
            item11.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8011.jpg");
            item11.setLatitude(37.7742);
            item11.setLongitude(-122.4179);
            item11.setVerificationQuestion("What brand initials are engraved on the wooden handle base?");
            item11.setUser(rohan);

            // 12. Trek FX 2 Bicycle (REUNITED)
            FoundItem item12 = new FoundItem(
                    "Trek FX 2 Disc City Commuter Bicycle (Matte Black)",
                    "Matte black Trek FX 2 Disc commuter bike with rear luggage rack and mounted front LED headlight.",
                    "OTHER",
                    LocalDate.now().minusDays(7),
                    "Student Center - West Bike Pavilion",
                    "REUNITED",
                    "Bike Shed A",
                    "Show lock key or purchase receipt"
            );
            item12.setImageUrl("/uploads/d1b7a240-6f81-423c-91d1-61019a6d8012.jpg");
            item12.setLatitude(37.7748);
            item12.setLongitude(-122.4210);
            item12.setCustodyDesk("Campus Police & Parking Office");
            item12.setStorageBin("BIN-BIKE-004");
            item12.setVerificationQuestion("What is the bicycle frame serial number stamped beneath bottom bracket?");
            item12.setUser(rohan);

            itemRepository.saveAll(List.of(item1, item2, item3, item4, item5, item6, item7, item8, item9, item10, item11, item12));

            // Seed Claims
            Claim claim1 = new Claim(rohan, item3, "Wallet has my student ID card (Rohan Sharma) and blue transit pass in left slot.", "APPROVED");
            claim1.setHandoverCode("582914");
            claim1.setHandoverPin("582914");
            claim1.setHandoverVerified(false);
            claim1.setVerificationAnswer("Student ID inside");
            claim1.setCreatedAt(LocalDateTime.now().minusDays(4));

            Claim claim2 = new Claim(david, item6, "Paired to my iPhone named 'David iPhone' with serial ending in 9F21.", "APPROVED");
            claim2.setHandoverCode("491028");
            claim2.setHandoverPin("491028");
            claim2.setHandoverVerified(true);
            claim2.setVerificationAnswer("David iPhone");
            claim2.setCreatedAt(LocalDateTime.now().minusDays(6));
            claim2.setResolvedAt(LocalDateTime.now().minusDays(5));

            Claim claim3 = new Claim(sneha, item10, "Card displays student ID number 2024-ENG-0881 and name Sneha Patel.", "APPROVED");
            claim3.setHandoverCode("739105");
            claim3.setHandoverPin("739105");
            claim3.setHandoverVerified(false);
            claim3.setVerificationAnswer("Student ID number 2024-ENG-0881");
            claim3.setCreatedAt(LocalDateTime.now().minusDays(4));

            Claim claim4 = new Claim(abinav, item12, "Trek FX 2 frame number WTU304C0918T with red Bell water bottle cage.", "APPROVED");
            claim4.setHandoverCode("615842");
            claim4.setHandoverPin("615842");
            claim4.setHandoverVerified(true);
            claim4.setVerificationAnswer("WTU304C0918T");
            claim4.setCreatedAt(LocalDateTime.now().minusDays(7));
            claim4.setResolvedAt(LocalDateTime.now().minusDays(6));

            claimRepository.saveAll(List.of(claim1, claim2, claim3, claim4));

            log.info("Successfully seeded 12 catalog items and 4 demo claims.");
        }
    }
}
