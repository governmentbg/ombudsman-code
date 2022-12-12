<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateQuestTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('q_quest', function (Blueprint $table) {

            $table->increments('Q_id');

            $table->string('Q_name', 255);
            $table->string('Q_desc', 1000);
            $table->date('Q_date_from')->index();
            $table->date('Q_date_to')->index();
            $table->boolean('St_id')->comment('Status')->nullable()->index();

            $table->timestamps();
            $table->softDeletes();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('q_quest');
    }
}
