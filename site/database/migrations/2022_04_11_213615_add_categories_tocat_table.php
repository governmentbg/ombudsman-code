<?php

use App\Models\MCategory;
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddCategoriesTocatTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_category', function (Blueprint $table) {

            MCategory::create([
                'Cat_name' => 'Новини',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Често задавани въпроси',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Омбудсманът в медиите',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => '',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Становища и искания до Конституционния съд',
                'Cat_parent_id' => null,
            ]);

            MCategory::create([
                'Cat_name' => 'Доклади',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Инициативи',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Публикации',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Гражданите за омбудсмана',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Добро управление в общините   ',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Инициативи, свързани с доброто управление',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Обобщени резултати от измерването на Индекса на доброто управление в местната власт',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Оценявани области на доброто управление',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Анкетирани групи',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Резултати от оценката на прилагането на принципите на добро управление в конкретни общини',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Методика за оценка на добро управление в общината',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Таблици за оценка',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Общини подписали протокол за прилагане на принципите на добро управление',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Добри практики',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Доброто управление в България',
                'Cat_parent_id' => 10,
            ]);
            MCategory::create([
                'Cat_name' => 'Годишни финансови отчети',
                'Cat_parent_id' => 6,
            ]);
            MCategory::create([
                'Cat_name' => 'Национален превантивен механизъм   ',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Профил на купувача ',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => '',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Екип',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Финансови документи',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'test',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Процедура за избор на заместник-омбудсман',
                'Cat_parent_id' => null,
            ]);
            MCategory::create([
                'Cat_name' => 'Съвет за наблюдение на КПХУ    ',
                'Cat_parent_id' => null,
            ]);
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_vcategory', function (Blueprint $table) {
            //
        });
    }
}
