<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMEventLngTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_event_lng', function (Blueprint $table) {
            $table->increments('MvL_id');
            $table->integer('Mv_id')->comment('Event ID')->unsigned();
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned();
            $table->string('MvL_path', 90)->index()->nullable();
            $table->string('MvL_title', 255)->nullable();

            $table->text('MvL_body')->nullable();
            $table->string('MvL_meta', 300)->nullable();

            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Mv_id')->references('Mv_id')->on('m_event');
            $table->foreign('S_Lng_id')->references('S_Lng_id')->on('s_lang');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_event_lng');
    }
}
